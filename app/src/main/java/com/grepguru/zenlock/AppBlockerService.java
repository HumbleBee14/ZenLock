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
import com.grepguru.zenlock.utils.WhitelistManager;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {
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
            // Log.d("AppBlockerService", "Event from our own LockScreenActivity or app. Ignoring.");
            return;
        }
        
        // IMMEDIATE BLOCK: Block launcher classes that bypass the lock
        if (isLauncherBypassClass(className)) {
            Log.d("AppBlockerService", "🚫 BLOCKING LAUNCHER BYPASS: " + packageName + " | Class: " + className);
            // Immediately launch lock screen without any delay
            launchLockScreen();
            return;
        }

        // Skip if this is the same package we just processed recently (debouncing)
        long currentTime = System.currentTimeMillis();
        if (packageName.equals(lastForegroundPackage) && (currentTime - lastForegroundCheckTime) < FOREGROUND_CHECK_DEBOUNCE_MS) {
            // Only skip if the package is the same and not the launcher (so launcher is always processed)
            if (!AppUtils.isLauncherPackage(this, packageName)) {
                return; // Skip processing the same package too frequently
            }
        }
        lastForegroundPackage = packageName;
        lastForegroundCheckTime = currentTime;
        
        boolean isAllowed = WhitelistManager.isAppWhitelisted(this, packageName);
        
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
            Log.d("AppBlockerService", "🔍 CURRENT APP: " + packageName + " | Class: " + className + " | Allowed: " + isAllowed);
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

    
    private boolean isThirdPartySystemIntegration(String packageName) {
        // Future: Add special handling for system-integrated third-party apps
        // Example: Apps that provide system-level services but need temporary access
        // Currently empty - all apps must be explicitly whitelisted by user
        
        String[] specialSystemIntegrations = {
            // Add packages here only if they provide essential system services
            // that need temporary access (e.g., emergency services, accessibility)
        };
        
        for (String specialApp : specialSystemIntegrations) {
            if (specialApp.equals(packageName)) {
                return true;
            }
        }
        
        return false; // No automatic allowances - user choice only
    }

    private void launchLockScreen() {
        try {
            // Double-check that system lock screen is not active before launching
            if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(this, "System Keyguard is active. Not launching LockScreenActivity.")) {
                return;
            }

            Intent intent = new Intent(this, LockScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            // Log.d("AppBlockerService", "LockScreenActivity launched to block unauthorized app access.");
        } catch (Exception e) {
            Log.e("AppBlockerService", "Failed to launch LockScreenActivity", e);
        }
    }

    @Override
    public void onInterrupt() {
    }

    private boolean isLauncherBypassClass(String className) {
        // Block specific launcher classes that can bypass the lock
        String[] bypassClasses = {
            "com.android.quickstep.RecentsActivity",        // Recent Activity button
            "com.sec.android.app.launcher.Launcher",        // Samsung Home launcher
            "com.android.launcher3.Launcher",               // AOSP Launcher3
            "com.google.android.launcher.GEL",              // Google Now Launcher
            "com.miui.home.launcher.Launcher",              // MIUI Launcher
            "com.oneplus.launcher.Launcher",                // OnePlus Launcher
            "com.huawei.android.launcher.Launcher",         // Huawei Launcher
            "com.oppo.launcher.Launcher",                   // OPPO Launcher
            "com.vivo.launcher.Launcher",                   // Vivo Launcher
            "com.samsung.android.launcher.Launcher",        // Samsung Launcher
            "com.android.quickstep.views.RecentsView",      // Recent Activity view
            "com.android.systemui.recents.RecentsActivity"  // SystemUI Recent Activity
        };
        
        for (String bypassClass : bypassClasses) {
            if (className.contains(bypassClass)) {
                return true;
            }
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
