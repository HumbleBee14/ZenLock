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
            String packageName = event.getPackageName().toString();
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
        
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        
        // Add default apps (phone, SMS, alarm) to the whitelist
        Set<String> allAllowedApps = new HashSet<>(whitelistedApps);
        allAllowedApps.addAll(AppUtils.getDefaultApps(this));
        
        boolean isAllowed = allAllowedApps.contains(packageName);
        Log.d("AppBlockerService", "Checking app: " + packageName + " - Allowed: " + isAllowed);
        return isAllowed;
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
