package com.grepguru.zenlock.permissions;

import com.grepguru.zenlock.ui.AppLanguage;
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
    ACCESSIBILITY(R.string.permission_accessibility_title, R.string.permission_accessibility_reason, R.drawable.ic_accessibility) {
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
            AccessibilityDisclosureDialog.show((androidx.fragment.app.FragmentActivity) activity);
        }
    },
    OVERLAY(R.string.permission_overlay_title, R.string.permission_overlay_reason, R.drawable.ic_overlay) {
        @Override
        public boolean isGranted(Context context) {
            return Settings.canDrawOverlays(context);
        }

        @Override
        public void request(Activity activity) {
            open(activity, packageIntent(activity, Settings.ACTION_MANAGE_OVERLAY_PERMISSION), null);
        }
    },
    NOTIFICATION_ACCESS(R.string.permission_notification_access_title, R.string.permission_notification_access_reason, R.drawable.ic_message) {
        @Override
        public boolean isGranted(Context context) {
            String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return listeners != null && listeners.contains(context.getPackageName());
        }

        @Override
        public void request(Activity activity) {
            open(activity, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), AppLanguage.localizedContext(activity).getString(R.string.background_turn_on_zenlock_in_the_list));
        }
    },
    EXACT_ALARM(R.string.permission_exact_alarm_title, R.string.permission_exact_alarm_reason, R.drawable.ic_alarm) {
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
    UNRESTRICTED_BATTERY(R.string.permission_unrestricted_battery_title, R.string.permission_unrestricted_battery_reason, R.drawable.ic_battery_protect) {
        @Override
        public boolean isGranted(Context context) {
            return BatteryOptimizationManager.isExempt(context);
        }

        @Override
        public void request(Activity activity) {
            BatteryOptimizationManager.requestExemption(activity);
        }
    },
    USAGE_ACCESS(R.string.permission_usage_access_title, R.string.permission_usage_access_reason, R.drawable.ic_usage_new) {
        @Override
        public boolean isGranted(Context context) {
            return UsageStatsPermissionManager.hasUsageStatsPermission(context);
        }

        @Override
        public void request(Activity activity) {
            open(activity, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), AppLanguage.localizedContext(activity).getString(R.string.background_turn_on_zenlock_in_the_list));
        }
    },
    XIAOMI_BACKGROUND_POPUP(R.string.permission_xiaomi_background_popup_title, R.string.permission_xiaomi_background_popup_reason, R.drawable.ic_overlay) {
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
            Toast.makeText(activity, AppLanguage.localizedContext(activity).getString(R.string.background_allow_display_pop_up_windows_while_running_in_background), Toast.LENGTH_LONG).show();
        }
    };

    public final int title;
    public final int reason;
    @DrawableRes
    public final int icon;

    AppPermission(int title, int reason, @DrawableRes int icon) {
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
            Toast.makeText(activity, AppLanguage.localizedContext(activity).getString(R.string.background_couldn_t_open_settings), Toast.LENGTH_SHORT).show();
        }
    }
}
