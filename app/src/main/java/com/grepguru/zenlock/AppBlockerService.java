package com.grepguru.zenlock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.grepguru.zenlock.utils.AppUtils;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.KeyguardUtils;
import com.grepguru.zenlock.utils.MiuiUtils;
import com.grepguru.zenlock.utils.WhitelistManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {
    private static final Set<String> LAUNCHER_PACKAGES = new HashSet<>(Arrays.asList(
        // Samsung
        "com.sec.android.app.launcher",           // Samsung One UI Launcher
        "com.samsung.android.launcher",           // Samsung Launcher (legacy)
        // Google / AOSP
        "com.google.android.apps.nexuslauncher",  // Pixel Launcher
        "com.android.launcher",                   // Stock Android Launcher (legacy)
        "com.android.launcher2",                  // AOSP Launcher2 (legacy)
        "com.android.launcher3",                  // AOSP Launcher3
        "com.google.android.launcher",            // Google Now Launcher (legacy)
        // Xiaomi / Redmi / POCO
        "com.miui.home",                          // MIUI / HyperOS Launcher
        "com.mi.android.globallauncher",          // POCO Launcher
        // OnePlus
        "com.oneplus.launcher",                   // OnePlus Launcher (OxygenOS 13+)
        "net.oneplus.launcher",                   // OnePlus Launcher (older OxygenOS)
        // Huawei / Honor
        "com.huawei.android.launcher",            // Huawei EMUI Launcher
        "com.hihonor.android.launcher",           // Honor MagicOS Launcher
        // Oppo / Realme
        "com.oppo.launcher",                      // OPPO ColorOS / Realme UI Launcher
        "com.realme.launcher",                    // Realme Launcher (legacy)
        // Vivo
        "com.bbk.launcher2",                      // Vivo FuntouchOS / OriginOS Launcher
        "com.vivo.launcher",                      // Vivo Launcher (legacy)
        // Nothing
        "com.nothing.launcher",                   // Nothing Phone Launcher
        // Motorola
        "com.motorola.launcher3",                 // Moto Launcher
        "com.motorola.launcher",                  // Moto Launcher (legacy)
        // Nokia (HMD)
        "com.hmd.launcher",                       // Nokia Launcher
        // ASUS
        "com.asus.launcher",                      // ASUS ZenUI / ROG Launcher
        // Lenovo
        "com.lenovo.launcher",                    // Lenovo Launcher
        // Sony
        "com.sonymobile.home",                    // Sony Xperia Home (older)
        "com.sonymobile.launcher",                // Sony Xperia Launcher (newer)
        "com.sony.launcher",                      // Sony Launcher (legacy)
        // LG (legacy)
        "com.lge.launcher2",                      // LG Launcher (older)
        "com.lge.launcher3",                      // LG Launcher (newer)
        // HTC
        "com.htc.launcher",                       // HTC Sense Home
        "com.htc.launcher.edge",                  // HTC Edge Launcher
        // Tecno / Infinix / itel (Transsion)
        "com.transsion.hilauncher",               // Tecno HiOS Launcher
        "com.transsion.XOSLauncher",              // Infinix XOS Launcher
        "com.transsion.itel.launcher",            // itel Launcher
        // ZTE / Nubia
        "com.zte.mifavor.launcher",               // ZTE MiFavor Launcher
        "com.nubia.launcher",                     // Nubia Launcher
        // Third-party launchers
        "com.nova.launcher",                      // Nova Launcher
        "com.teslacoilsw.launcher",               // Nova Launcher (alternative pkg)
        "com.microsoft.launcher",                 // Microsoft Launcher
        "com.anddoes.launcher",                   // ADW Launcher
        "com.go.launcher",                        // GO Launcher
        "com.apex.launcher",                      // Apex Launcher
        "com.lx.launcher8"                        // Launcher 8
    ));
    private String lastLoggedPackage = "";
    private long lastLogTime = 0;
    private static final long LOG_DEBOUNCE_MS = 1000; // Only log same package once per second
    private String lastForegroundPackage = "";
    private long lastForegroundCheckTime = 0;
    private static final long FOREGROUND_CHECK_DEBOUNCE_MS = 100; // Reduced debounce for instant response
    private AnalyticsManager analyticsManager;
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        // CRITICAL CHECK: If system lock screen (Keyguard) is active, do nothing
        // This prevents conflicts and infinite loops when the system lock screen is displayed
        if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(this, "System Keyguard is active. AppBlockerService will not interfere.")) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);
        boolean allowLauncherDuringLock = preferences.getBoolean("allow_launcher_during_lock", false);

        if (!isLocked) {
            return; // No focus session active, nothing to block
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        
        // Skip if package name is empty or null
        if (packageName.isEmpty()) {
            return;
        }

        // Skip if the event is from our own LockScreenActivity to prevent self-blocking loops
        if (className.contains("LockScreenActivity") || packageName.equals(getApplicationContext().getPackageName())) {
            return;
        }
        // IMMEDIATE BLOCK: Block launcher classes that bypass the lock
        if (isLauncherBypassClass(className)) {
            if (!allowLauncherDuringLock) {
                Log.d("AppBlockerService", "🚫 BLOCKING LAUNCHER BYPASS: " + packageName + " | Class: " + className);
                launchLockScreen();
                return;
            } // else: allow launcher bypass if user enabled
        }

        // Skip if this is the same package we just processed recently (debouncing)
        long currentTime = System.currentTimeMillis();
        if (packageName.equals(lastForegroundPackage) && (currentTime - lastForegroundCheckTime) < FOREGROUND_CHECK_DEBOUNCE_MS) {
            // Only skip if the package is the same and not the launcher (so launcher is always processed)
            if (!AppUtils.isLauncherPackage(this, packageName)) {
                return; // Skip processing the same package too frequently
            }
        }
        // -----------------------------------
        lastForegroundPackage = packageName;
        lastForegroundCheckTime = currentTime;
        
        // FIRST: Check if this is a launcher package (skip whitelist check for these)
        boolean isLauncherPackage = isLauncherPackage(packageName);
        // SECOND: Check if this is a specific launcher/recent activity class that should be blocked
        boolean isLauncherBypass = isLauncherBypassClass(className);
        // THIRD: Determine if allowed
        boolean isAllowed;
        if (isLauncherPackage) {
            // For launcher packages, only block specific classes (like Launcher, RecentsActivity)
            isAllowed = allowLauncherDuringLock || !isLauncherBypass;
        } else {
            // For non-launcher packages, check whitelist
            isAllowed = WhitelistManager.isAppWhitelisted(this, packageName);
        }
        
        // Track analytics
        if (analyticsManager != null && analyticsManager.hasActiveSession()) {
            if (isAllowed) {
                analyticsManager.recordAppAccess(packageName);
            } else {
                analyticsManager.recordBlockedAttempt(packageName);
            }
        }
        
        // Log EVERY package event for debugging (with debouncing to prevent spam)
        if (!packageName.equals(lastLoggedPackage) || (currentTime - lastLogTime) > LOG_DEBOUNCE_MS) {
            Log.d("AppBlockerService", "🔍 CURRENT APP: " + packageName + " | Class: " + className + " | Allowed: " + isAllowed + (isLauncherBypass ? " (Launcher Bypass Blocked)" : ""));
            lastLoggedPackage = packageName;
            lastLogTime = currentTime;
        }
        
        // Also log touch events specifically
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED || 
            event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
            Log.d("AppBlockerService", "👆 TOUCH EVENT on: " + packageName + " | Type: " + getEventTypeName(event.getEventType()));
        }
        
        if (!isAllowed) {
            // Aggressively show overlay and lock screen
            OverlayLockService.showOverlay(this); // Ensure overlay is shown instantly
            launchLockScreen();
        } else {
            // Mark that we allowed a whitelisted app to prevent LockScreenActivity from restarting
            SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("lastWhitelistedAppTime", System.currentTimeMillis());
            editor.apply();
            // Log.d("AppBlockerService", "Marked whitelisted app access time: " + packageName);
        }
    }


    private void launchLockScreen() {
        try {
            // Double-check that system lock screen is not active before launching
            if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(this, "System Keyguard is active. Not launching LockScreenActivity.")) {
                return;
            }

            // Always show overlay first — this works even when startActivity is blocked
            OverlayLockService.showOverlay(this);

            // On MIUI/HyperOS, startActivity() from background is silently blocked
            // unless "Display pop-up windows while running in background" is enabled.
            // Use full-screen intent notification as fallback which MIUI does NOT block.
            if (!MiuiUtils.canStartActivityFromBackground(this)) {
                Log.d("AppBlockerService", "MIUI detected with background start blocked — using notification fallback");
                LockScreenLauncher.launchFromBlocker(this);
                return;
            }

            Intent intent = new Intent(this, LockScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("AppBlockerService", "Failed to launch LockScreenActivity, trying notification fallback", e);
            // Fallback for any OEM that silently blocks without throwing
            LockScreenLauncher.launchFromBlocker(this);
        }
    }

    @Override
    public void onInterrupt() {
    }

    private boolean isLauncherPackage(String packageName) {
        return LAUNCHER_PACKAGES.contains(packageName);
    }

    private boolean isLauncherBypassClass(String className) {
        // Block launcher classes that can bypass the lock
        // Block any class containing "Launcher" keyword (covers custom launchers)
        // Block specific recent activity classes

        String classLower = className.toLowerCase();

        // Block any class containing "Launcher" (covers all launchers)
        if (classLower.contains("launcher")) {
            return true;
        }

        // Block any class containing "Recents" (covers OEM recents screens)
        if (classLower.contains("recents")) {
            return true;
        }

        return false;
    }

    private String getEventTypeName(int eventType) {
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "CLICK";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "LONG_CLICK";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "WINDOW_CHANGE";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "FOCUS";
            default:
                return "OTHER(" + eventType + ")";
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        // Initialize analytics manager
        analyticsManager = new AnalyticsManager(this);
        
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_VIEW_CLICKED | 
                         AccessibilityEvent.TYPE_VIEW_LONG_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }
}
