package com.grepguru.zenlock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
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
    private AnalyticsManager analyticsManager;
    private SharedPreferences sessionPreferences;
    private boolean launchPending;
    private boolean blockedWindow;
    private final android.os.Handler sessionHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable presentLockScreen = this::presentLockScreen;
    private final Runnable verifyLockScreen = () -> {
        launchPending = false;
        if (!blockedWindow || LockScreenActivity.isVisible() || !shouldEnforce()) return;
        LockScreenLauncher.launchFromBlocker(this);
    };
    private final Runnable activateSession = () -> {
        if (sessionPreferences == null) return;
        if (!sessionPreferences.getString("current_session_source", "").startsWith("schedule:")) return;
        if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(this, "Wait for device unlock before presenting a scheduled session")) return;
        if (sessionPreferences.getBoolean("isLocked", false)
                && sessionPreferences.getLong("lockEndTime", 0) > System.currentTimeMillis()) {
            analyticsManager = new AnalyticsManager(this);
            blockedWindow = true;
            launchLockScreen();
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener sessionListener = (prefs, key) -> {
        if ("isLocked".equals(key)) {
            if (!prefs.getBoolean("isLocked", false)) clearPendingBlock();
            sessionHandler.removeCallbacks(activateSession);
            sessionHandler.post(activateSession);
        }
    };

    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Click/focus events can come from transient or background UI; they are not
        // evidence that the foreground application changed.
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!shouldEnforce()) {
            clearPendingBlock();
            return;
        }
        String packageName = event.getPackageName() == null ? "" : event.getPackageName().toString();
        if (packageName.isEmpty()) return;
        String className = event.getClassName() == null ? "" : event.getClassName().toString();
        if (packageName.equals(getPackageName())) {
            clearPendingBlock();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        boolean allowLauncher = prefs.getBoolean("allow_launcher_during_lock", false);
        // Launcher identity must come from its package / HOME intent, never an
        // arbitrary app's activity name (e.g. an allowed app's LauncherActivity).
        boolean launcher = LAUNCHER_PACKAGES.contains(packageName) || AppUtils.isLauncherPackage(this, packageName);
        boolean systemRecents = "com.android.systemui".equals(packageName)
                && className.toLowerCase(java.util.Locale.ROOT).contains("recents");
        boolean allowed = (launcher || systemRecents) ? allowLauncher
                : WhitelistManager.isAppWhitelisted(this, packageName);

        if (analyticsManager != null && analyticsManager.hasActiveSession()) {
            if (allowed) analyticsManager.recordAppAccess(packageName);
            else analyticsManager.recordBlockedAttempt(packageName);
        }
        long now = System.currentTimeMillis();
        if (!packageName.equals(lastLoggedPackage) || now - lastLogTime > LOG_DEBOUNCE_MS) {
            Log.d("AppBlockerService", "Window: " + packageName + " | Class: " + className + " | Allowed: " + allowed);
            lastLoggedPackage = packageName;
            lastLogTime = now;
        }
        if (allowed) {
            // A keyboard / notification shade is a supporting window, not proof
            // that the blocked app underneath it has been left.
            if (!launcher && !systemRecents && (WhitelistManager.isEnabledKeyboard(this, packageName)
                    || "com.android.systemui".equals(packageName) || "android".equals(packageName))) return;
            clearPendingBlock();
        } else {
            blockedWindow = true;
            launchLockScreen();
        }
    }

    private boolean shouldEnforce() {
        android.os.PowerManager power = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        return getSharedPreferences("FocusLockPrefs", MODE_PRIVATE).getBoolean("isLocked", false)
                && (power == null || power.isInteractive())
                && !KeyguardUtils.isKeyguardLocked(this);
    }

    private void clearPendingBlock() {
        blockedWindow = false;
        launchPending = false;
        sessionHandler.removeCallbacks(presentLockScreen);
        sessionHandler.removeCallbacks(verifyLockScreen);
        android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
        if (manager != null) manager.cancel(9999);
    }

    private void launchLockScreen() {
        if (!shouldEnforce() || launchPending) return;
        launchPending = true;
        // Let the destination window and activity lifecycle settle. An allowed
        // app event cancels this, including transient launcher windows en route.
        sessionHandler.postDelayed(presentLockScreen, 100);
    }

    private void presentLockScreen() {
        // Existing-but-paused and actually-visible screens are different states.
        // Never replace the visible PIN screen, or queue launches for an event burst.
        if (!blockedWindow || !shouldEnforce() || LockScreenActivity.isVisible()) {
            launchPending = false;
            return;
        }
        try {
            if (!MiuiUtils.canStartActivityFromBackground(this)) {
                LockScreenLauncher.launchFromBlocker(this);
            } else {
                Intent intent = new Intent(this, LockScreenActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        } catch (RuntimeException e) {
            Log.e("AppBlockerService", "Direct launch failed; using notification fallback", e);
            LockScreenLauncher.launchFromBlocker(this);
        }
        sessionHandler.removeCallbacks(verifyLockScreen);
        sessionHandler.postDelayed(verifyLockScreen, 1500);
    }

    @Override
    public void onInterrupt() {
        clearPendingBlock();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        // Initialize analytics manager
        analyticsManager = new AnalyticsManager(this);
        
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        if (sessionPreferences != null) sessionPreferences.unregisterOnSharedPreferenceChangeListener(sessionListener);
        sessionPreferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        sessionPreferences.registerOnSharedPreferenceChangeListener(sessionListener);
        sessionHandler.removeCallbacks(activateSession);
        sessionHandler.post(activateSession);
    }
    @Override
    public void onDestroy() {
        clearPendingBlock();
        sessionHandler.removeCallbacks(activateSession);
        if (sessionPreferences != null) sessionPreferences.unregisterOnSharedPreferenceChangeListener(sessionListener);
        sessionPreferences = null;
        super.onDestroy();
    }
}
