package com.grepguru.zenlock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.grepguru.zenlock.utils.AppUtils;
import com.grepguru.zenlock.utils.AnalyticsManager;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {
    private String lastLoggedPackage = "";
    private long lastLogTime = 0;
    private static final long LOG_DEBOUNCE_MS = 1000; // Only log same package once per second
    private String lastForegroundPackage = "";
    private long lastForegroundCheckTime = 0;
    private static final long FOREGROUND_CHECK_DEBOUNCE_MS = 2000; // Check foreground app every 2 seconds
    private AnalyticsManager analyticsManager;
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        // CRITICAL CHECK: If system lock screen (Keyguard) is active, do nothing
        // This prevents conflicts and infinite loops when the system lock screen is displayed
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            Log.d("AppBlockerService", "System Keyguard is active. AppBlockerService will not interfere.");
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
            Log.d("AppBlockerService", "Event from our own LockScreenActivity or app. Ignoring.");
            return;
        }

        // Skip if this is the same package we just processed recently (debouncing)
        long currentTime = System.currentTimeMillis();
        if (packageName.equals(lastForegroundPackage) && (currentTime - lastForegroundCheckTime) < FOREGROUND_CHECK_DEBOUNCE_MS) {
            return; // Skip processing the same package too frequently
        }
        lastForegroundPackage = packageName;
        lastForegroundCheckTime = currentTime;
        
        boolean isAllowed = isAllowedApp(packageName);
        
        // Track analytics
        if (analyticsManager != null && analyticsManager.hasActiveSession()) {
            if (isAllowed) {
                analyticsManager.recordAppAccess(packageName);
            } else {
                analyticsManager.recordBlockedAttempt(packageName);
            }
        }
        
        // Log package events for debugging (with debouncing to prevent spam)
        if (!packageName.equals(lastLoggedPackage) || (currentTime - lastLogTime) > LOG_DEBOUNCE_MS) {
            Log.d("AppBlockerService", "Event from package: " + packageName + " - Allowed: " + isAllowed);
            lastLoggedPackage = packageName;
            lastLogTime = currentTime;
        }
        
        if (!isAllowed) {
            launchLockScreen();
        } else {
            // Mark that we allowed a whitelisted app to prevent LockScreenActivity from restarting
            SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("lastWhitelistedAppTime", System.currentTimeMillis());
            editor.apply();
            Log.d("AppBlockerService", "Marked whitelisted app access time: " + packageName);
        }
    }

    private boolean isAllowedApp(String packageName) {
        // Always allow the ZenLock app itself
        if ("com.grepguru.zenlock".equals(packageName)) {
            return true;
        }
        
        // Allow essential system packages to prevent conflicts
        String[] essentialSystemPackages = {
            "com.android.systemui",           // System UI (status bar, navigation, etc.)
            "com.android.keyguard",           // System lock screen
            "android",                        // Core Android system
            "com.android.settings",           // System settings
            "com.android.phone",              // Phone app (for emergency calls)
            "com.android.incallui",           // In-call UI
            "com.android.dialer",             // Dialer app
            "com.android.emergency",          // Emergency services
            "com.android.camera2",            // Camera (for emergency photos)
            "com.android.camera",             // Camera (alternative)
            "com.google.android.gms",         // Google Play Services
            "com.google.android.gsf"          // Google Services Framework
        };
        
        for (String systemPackage : essentialSystemPackages) {
            if (systemPackage.equals(packageName)) {
                return true;
            }
        }
        
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        
        // Get ALL allowed packages (including system services for in-app activities)
        Set<String> allAllowedApps = new HashSet<>(whitelistedApps);
        allAllowedApps.addAll(AppUtils.getAllAllowedPackages(this));
        
        return allAllowedApps.contains(packageName);
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
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
                Log.d("AppBlockerService", "System Keyguard is active. Not launching LockScreenActivity.");
                return;
            }

            Intent intent = new Intent(this, LockScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Log.d("AppBlockerService", "LockScreenActivity launched to block unauthorized app access.");
        } catch (Exception e) {
            Log.e("AppBlockerService", "Failed to launch LockScreenActivity", e);
        }
    }

    @Override
    public void onInterrupt() {
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
    }
}
