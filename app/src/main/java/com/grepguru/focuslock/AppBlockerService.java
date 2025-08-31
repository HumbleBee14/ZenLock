package com.grepguru.focuslock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.grepguru.focuslock.utils.AppUtils;
import com.grepguru.focuslock.utils.AnalyticsManager;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {
    private String lastLoggedPackage = "";
    private long lastLogTime = 0;
    private static final long LOG_DEBOUNCE_MS = 1000; // Only log same package once per second
    private AnalyticsManager analyticsManager;
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);

        if (isLocked) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // Skip if package name is empty or null
            if (packageName.isEmpty()) {
                return;
            }
            
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
            long currentTime = System.currentTimeMillis();
            if (!packageName.equals(lastLoggedPackage) || (currentTime - lastLogTime) > LOG_DEBOUNCE_MS) {
                Log.d("AppBlockerService", "Event from package: " + packageName + " - Allowed: " + isAllowed);
                lastLoggedPackage = packageName;
                lastLogTime = currentTime;
            }
            
            if (!isAllowed) {
                launchLockScreen();
            }
        }
    }

    private boolean isAllowedApp(String packageName) {
        // Always allow the Focus Lock app itself
        if ("com.grepguru.focuslock".equals(packageName)) {
            return true;
        }
        
        // Future: Handle special system integrations if needed
        // Currently disabled - all third-party apps must be explicitly whitelisted by user
        
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
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
