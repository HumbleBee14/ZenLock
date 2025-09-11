package com.grepguru.zenlock.utils;

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
                .setTitle("Schedule Permission Required")
                .setMessage("ZenLock needs this permission to ensure your focus sessions begin at the scheduled time.\n\n" +
                           "You'll be taken to Android Settings to grant this permission.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    openExactAlarmSettings(activity);
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    Toast.makeText(activity, "Scheduled focus sessions may not work without this permission", Toast.LENGTH_LONG).show();
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
                Toast.makeText(context, "Please enable exact alarm permission in Settings > Apps > ZenLock > Permissions", Toast.LENGTH_LONG).show();
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
