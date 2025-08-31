package com.grepguru.focuslock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.grepguru.focuslock.utils.AppUtils;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {
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
            
            // Log package events for debugging (essential for troubleshooting)
            Log.d("AppBlockerService", "Event from package: " + packageName);
            
            if (!isAllowedApp(packageName)) {
                launchLockScreen();
            }
        }
    }

    private boolean isAllowedApp(String packageName) {
        // Always allow the Focus Lock app itself
        if ("com.grepguru.focuslock".equals(packageName)) {
            return true;
        }
        
        // Handle third-party apps that integrate with system functions
        if (isThirdPartySystemIntegration(packageName)) {
            // Allow briefly but return to lock screen after a short delay
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(() -> {
                if (getSharedPreferences("FocusLockPrefs", MODE_PRIVATE).getBoolean("isLocked", false)) {
                    launchLockScreen();
                }
            }, 3000); // 3 second delay
            return true;
        }
        
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        
        // Get ALL allowed packages (including system services for in-app activities)
        Set<String> allAllowedApps = new HashSet<>(whitelistedApps);
        allAllowedApps.addAll(AppUtils.getAllAllowedPackages(this));
        
        return allAllowedApps.contains(packageName);
    }
    
    private boolean isThirdPartySystemIntegration(String packageName) {
        // Third-party apps that integrate with system functions (calls, SMS)
        // These should be allowed briefly but not permanently
        String[] thirdPartyIntegrations = {
            "com.truecaller", // Caller ID service
            "com.whatsapp", // If user has WhatsApp as default SMS
            "com.viber.voip", // Viber calls
            "com.skype.raider" // Skype calls
        };
        
        for (String thirdParty : thirdPartyIntegrations) {
            if (thirdParty.equals(packageName)) {
                return true;
            }
        }
        
        return false;
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
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }
}
