package com.grepguru.zenlock.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.annotation.DrawableRes;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.utils.AlarmPermissionManager;
import com.grepguru.zenlock.utils.BatteryOptimizationManager;
import com.grepguru.zenlock.utils.MiuiUtils;
import com.grepguru.zenlock.utils.UsageStatsPermissionManager;

import android.accessibilityservice.AccessibilityServiceInfo;

public enum AppPermission {
    ACCESSIBILITY("Accessibility service", "Closes blocked apps during a session", R.drawable.ic_accessibility) {
        @Override
        public boolean isGranted(Context context) {
            AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager == null) return false;
            for (AccessibilityServiceInfo service : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (service.getId().contains(context.getPackageName())) return true;
            }
            return false;
        }

        @Override
        public void request(Activity activity) {
            open(activity, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "Turn on ZenLock in the list");
        }
    },
    OVERLAY("Display over other apps", "Shows the lock screen over blocked apps", R.drawable.ic_overlay) {
        @Override
        public boolean isGranted(Context context) {
            return Settings.canDrawOverlays(context);
        }

        @Override
        public void request(Activity activity) {
            open(activity, packageIntent(activity, Settings.ACTION_MANAGE_OVERLAY_PERMISSION), null);
        }
    },
    NOTIFICATION_ACCESS("Notification access", "Hides notifications from blocked apps", R.drawable.ic_message) {
        @Override
        public boolean isGranted(Context context) {
            String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return listeners != null && listeners.contains(context.getPackageName());
        }

        @Override
        public void request(Activity activity) {
            open(activity, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), "Turn on ZenLock in the list");
        }
    },
    EXACT_ALARM("Exact alarms", "Starts schedules on time", R.drawable.ic_alarm) {
        @Override
        public boolean appliesTo(Context context) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        }

        @Override
        public boolean isGranted(Context context) {
            return AlarmPermissionManager.canScheduleExactAlarms(context);
        }

        @Override
        public void request(Activity activity) {
            open(activity, packageIntent(activity, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM), null);
        }
    },
    UNRESTRICTED_BATTERY("Unrestricted battery", "Keeps schedules running while asleep", R.drawable.ic_battery_protect) {
        @Override
        public boolean isGranted(Context context) {
            return BatteryOptimizationManager.isExempt(context);
        }

        @Override
        public void request(Activity activity) {
            BatteryOptimizationManager.requestExemption(activity);
        }
    },
    USAGE_ACCESS("Usage access", "Shows phone usage in Insights", R.drawable.ic_usage_new) {
        @Override
        public boolean isGranted(Context context) {
            return UsageStatsPermissionManager.hasUsageStatsPermission(context);
        }

        @Override
        public void request(Activity activity) {
            open(activity, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), "Turn on ZenLock in the list");
        }
    },
    XIAOMI_BACKGROUND_POPUP("Background pop-ups", "Opens the lock screen from the background", R.drawable.ic_overlay) {
        @Override
        public boolean appliesTo(Context context) {
            return MiuiUtils.isXiaomiDevice();
        }

        @Override
        public boolean isGranted(Context context) {
            return MiuiUtils.canStartActivityFromBackground(context);
        }

        @Override
        public void request(Activity activity) {
            MiuiUtils.openMiuiPermissionEditor(activity);
            Toast.makeText(activity, "Allow 'Display pop-up windows while running in background'", Toast.LENGTH_LONG).show();
        }
    };

    public final String title;
    public final String reason;
    @DrawableRes
    public final int icon;

    AppPermission(String title, String reason, @DrawableRes int icon) {
        this.title = title;
        this.reason = reason;
        this.icon = icon;
    }

    public boolean appliesTo(Context context) {
        return true;
    }

    public abstract boolean isGranted(Context context);

    public abstract void request(Activity activity);

    static Intent packageIntent(Activity activity, String action) {
        Intent intent = new Intent(action);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        return intent;
    }

    static void open(Activity activity, Intent intent, String hint) {
        try {
            activity.startActivity(intent);
            if (hint != null) Toast.makeText(activity, hint, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "Couldn't open Settings", Toast.LENGTH_SHORT).show();
        }
    }
}
