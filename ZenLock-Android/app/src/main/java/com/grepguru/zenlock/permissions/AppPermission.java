package com.grepguru.zenlock.permissions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.utils.AlarmPermissionManager;
import com.grepguru.zenlock.utils.BatteryOptimizationManager;
import com.grepguru.zenlock.utils.MiuiUtils;

import android.accessibilityservice.AccessibilityServiceInfo;

public enum AppPermission {
    ACCESSIBILITY("Accessibility service", "Detects and closes blocked apps during a session. Nothing leaves your device.", R.drawable.ic_accessibility) {
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
        public void request(PermissionHost host) {
            open(host, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "Turn on ZenLock in the list");
        }
    },
    OVERLAY("Display over other apps", "Shows the lock screen on top of a blocked app.", R.drawable.ic_overlay) {
        @Override
        public boolean isGranted(Context context) {
            return Settings.canDrawOverlays(context);
        }

        @Override
        public void request(PermissionHost host) {
            open(host, packageIntent(host, Settings.ACTION_MANAGE_OVERLAY_PERMISSION), null);
        }
    },
    NOTIFICATIONS("Notifications", "Keeps the session timer visible while you focus.", R.drawable.ic_message) {
        @Override
        public boolean appliesTo(Context context) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        }

        @Override
        public boolean isGranted(Context context) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public void request(PermissionHost host) {
            host.requestRuntimePermission(Manifest.permission.POST_NOTIFICATIONS);
        }
    },
    NOTIFICATION_ACCESS("Notification access", "Hides notifications from blocked apps during a session.", R.drawable.ic_message) {
        @Override
        public boolean isGranted(Context context) {
            String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return listeners != null && listeners.contains(context.getPackageName());
        }

        @Override
        public void request(PermissionHost host) {
            open(host, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), "Turn on ZenLock in the list");
        }
    },
    EXACT_ALARM("Exact alarms", "Starts sessions at the exact scheduled minute.", R.drawable.ic_alarm) {
        @Override
        public boolean appliesTo(Context context) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        }

        @Override
        public boolean isGranted(Context context) {
            return AlarmPermissionManager.canScheduleExactAlarms(context);
        }

        @Override
        public void request(PermissionHost host) {
            open(host, packageIntent(host, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM), null);
        }
    },
    UNRESTRICTED_BATTERY("Unrestricted battery", "Keeps schedules firing when the phone is asleep.", R.drawable.ic_battery_protect) {
        @Override
        public boolean isGranted(Context context) {
            return BatteryOptimizationManager.isExempt(context);
        }

        @Override
        public void request(PermissionHost host) {
            BatteryOptimizationManager.requestExemption(host.activity());
        }
    },
    XIAOMI_BACKGROUND_POPUP("Background pop-ups", "Lets the lock screen appear from the background on Xiaomi devices.", R.drawable.ic_overlay) {
        @Override
        public boolean appliesTo(Context context) {
            return MiuiUtils.isXiaomiDevice();
        }

        @Override
        public boolean isGranted(Context context) {
            return MiuiUtils.canStartActivityFromBackground(context);
        }

        @Override
        public void request(PermissionHost host) {
            MiuiUtils.openMiuiPermissionEditor(host.activity());
            Toast.makeText(host.activity(), "Allow 'Display pop-up windows while running in background'", Toast.LENGTH_LONG).show();
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

    public abstract void request(PermissionHost host);

    static Intent packageIntent(PermissionHost host, String action) {
        Intent intent = new Intent(action);
        intent.setData(Uri.fromParts("package", host.activity().getPackageName(), null));
        return intent;
    }

    static void open(PermissionHost host, Intent intent, String hint) {
        try {
            host.activity().startActivity(intent);
            if (hint != null) Toast.makeText(host.activity(), hint, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(host.activity(), "Open Settings and grant it manually", Toast.LENGTH_SHORT).show();
        }
    }
}
