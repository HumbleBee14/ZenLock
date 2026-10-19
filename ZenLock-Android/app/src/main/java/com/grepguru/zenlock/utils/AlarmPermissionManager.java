package com.grepguru.zenlock.utils;

import com.grepguru.zenlock.ui.AppLanguage;
import com.grepguru.zenlock.R;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AlarmPermissionManager - Handles SCHEDULE_EXACT_ALARM permission for Android 12+
 * Manages the permission request flow for exact alarm scheduling
 */
public class AlarmPermissionManager {
    
    private static final String TAG = "AlarmPermissionManager";
    
    /**
     * Check if the app can schedule exact alarms
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                boolean canSchedule = alarmManager.canScheduleExactAlarms();
                Log.d(TAG, "Can schedule exact alarms: " + canSchedule);
                return canSchedule;
            }
            return false;
        }
        // For Android < 12, exact alarms don't need special permission
        return true;
    }
    
    /**
     * Request exact alarm permission if needed
     */
    public static void requestExactAlarmPermission(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!canScheduleExactAlarms(activity)) {
                showExactAlarmPermissionDialog(activity);
            }
        }
    }
    
    /**
     * Show dialog explaining why exact alarm permission is needed
     */
    private static void showExactAlarmPermissionDialog(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(AppLanguage.localizedContext(activity).getString(R.string.background_schedule_permission_required))
                .setMessage(AppLanguage.localizedContext(activity).getString(R.string.background_zenlock_needs_this_permission_to_ensure_your_focus_sessions_begin_at_the_sc))
                .setPositiveButton(AppLanguage.localizedContext(activity).getString(R.string.background_grant_permission), (dialog, which) -> {
                    openExactAlarmSettings(activity);
                })
                .setNegativeButton(AppLanguage.localizedContext(activity).getString(R.string.background_skip), (dialog, which) -> {
                    Toast.makeText(activity, AppLanguage.localizedContext(activity).getString(R.string.background_scheduled_focus_sessions_may_not_work_without_this_permission), Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * Open Android settings for exact alarm permission
     */
    private static void openExactAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Open exact alarm permission settings
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                Log.d(TAG, "Opened exact alarm permission settings");
            } catch (Exception e) {
                Log.e(TAG, "Failed to open exact alarm settings", e);
                Toast.makeText(context, AppLanguage.localizedContext(context).getString(R.string.background_please_enable_exact_alarm_permission_in_settings_apps_zenlock_permissions), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Check permission and show guidance if needed
     */
    public static boolean checkAndRequestIfNeeded(AppCompatActivity activity) {
        if (canScheduleExactAlarms(activity)) {
            return true;
        }
        
        requestExactAlarmPermission(activity);
        return false;
    }
}
