package com.grepguru.zenlock;

import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.grepguru.zenlock.utils.AppUtils;

import java.util.Set;

public class NotificationBlockerService extends NotificationListenerService {

    private static final String TAG = "NotificationBlocker";
    private SharedPreferences.OnSharedPreferenceChangeListener prefsListener;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        clearBlockedNotifications();

        SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        prefsListener = (sharedPreferences, key) -> {
            if ("isLocked".equals(key) && sharedPreferences.getBoolean("isLocked", false)) {
                clearBlockedNotifications();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (prefsListener != null) {
            SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
            prefsListener = null;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        String packageName = sbn.getPackageName();
        if (packageName == null) return;

        if (!shouldBlockNotification(packageName)) return;

        try {
            cancelNotification(sbn.getKey());
            Log.d(TAG, "Blocked notification from: " + packageName);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to cancel notification: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    private void clearBlockedNotifications() {
        try {
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            if (activeNotifications == null) return;

            for (StatusBarNotification sbn : activeNotifications) {
                String pkg = sbn.getPackageName();
                if (pkg != null && shouldBlockNotification(pkg)) {
                    cancelNotification(sbn.getKey());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing notifications: " + e.getMessage());
        }
    }

    private boolean shouldBlockNotification(String packageName) {
        SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);

        boolean isLocked = prefs.getBoolean("isLocked", false);
        if (!isLocked) return false;

        boolean blockNotifications = prefs.getBoolean("block_notifications", true);
        if (!blockNotifications) return false;

        long lockEndTime = prefs.getLong("lockEndTime", 0);
        if (lockEndTime > 0 && System.currentTimeMillis() >= lockEndTime) return false;

        if ("com.grepguru.zenlock".equals(packageName)) return false;

        if ("android".equals(packageName) || "com.android.systemui".equals(packageName)) return false;

        if (isEssentialApp(packageName, prefs)) return false;

        Set<String> whitelistedApps = prefs.getStringSet("whitelisted_apps", null);
        if (whitelistedApps != null && whitelistedApps.contains(packageName)) return false;

        return true;
    }

    private boolean isEssentialApp(String packageName, SharedPreferences prefs) {
        if (prefs.getBoolean("allow_phone_app", true)) {
            String dialer = AppUtils.findMainDialerApp(this);
            if (packageName.equals(dialer)) return true;
        }

        if (prefs.getBoolean("allow_clock_app", true)) {
            String clock = AppUtils.findMainClockApp(this);
            if (packageName.equals(clock)) return true;
        }

        if (prefs.getBoolean("allow_calendar_app", true)) {
            String calendar = AppUtils.findMainCalendarApp(this);
            if (packageName.equals(calendar)) return true;
        }

        String defaultSms = android.provider.Telephony.Sms.getDefaultSmsPackage(this);
        if (packageName.equals(defaultSms)) return true;

        if (packageName.equals("com.android.phone") ||
            packageName.equals("com.android.incallui") ||
            packageName.equals("com.android.dialer") ||
            packageName.equals("com.android.emergency") ||
            packageName.equals("com.samsung.android.incallui") ||
            packageName.equals("com.samsung.android.dialer") ||
            packageName.equals("com.google.android.dialer")) {
            return true;
        }

        return false;
    }
}
