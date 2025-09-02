package com.grepguru.zenlock.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Manages notification permissions for Android 13+ (API 33+)
 * Handles permission requests and provides user-friendly guidance
 */
public class NotificationPermissionManager {
    
    private static final String TAG = "NotificationPermissionManager";
    
    // Permission constants
    public static final String NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS;
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    /**
     * Check if notification permission is granted
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, NOTIFICATION_PERMISSION) 
                   == PackageManager.PERMISSION_GRANTED;
        }
        // For older Android versions, notifications are enabled by default
        return true;
    }
    
    /**
     * Check if we should show permission rationale
     */
    public static boolean shouldShowPermissionRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, NOTIFICATION_PERMISSION);
        }
        return false;
    }
    
    /**
     * Request notification permission with proper handling
     */
    public static void requestNotificationPermission(Activity activity, 
                                                   ActivityResultLauncher<String> permissionLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission(activity)) {
                Log.d(TAG, "Notification permission already granted");
                return;
            }
            
            if (shouldShowPermissionRationale(activity)) {
                showPermissionRationaleDialog(activity, permissionLauncher);
            } else {
                // Request permission directly
                permissionLauncher.launch(NOTIFICATION_PERMISSION);
            }
        } else {
            Log.d(TAG, "Notification permission not required for this Android version");
        }
    }
    
    /**
     * Show permission rationale dialog explaining why we need notifications
     */
    private static void showPermissionRationaleDialog(Activity activity, 
                                                     ActivityResultLauncher<String> permissionLauncher) {
        new AlertDialog.Builder(activity)
                .setTitle("Notifications Required")
                .setMessage("ZenLock needs notification permission to:\n\n" +
                           "• Alert you before scheduled focus sessions\n" +
                           "• Notify when focus sessions start\n" +
                           "• Keep you informed about your focus schedule\n\n" +
                           "Without this permission, scheduled sessions won't work properly.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    permissionLauncher.launch(NOTIFICATION_PERMISSION);
                })
                .setNegativeButton("Not Now", null)
                .setNeutralButton("Settings", (dialog, which) -> {
                    openAppSettings(activity);
                })
                .show();
    }
    
    /**
     * Show settings dialog if permission is permanently denied
     */
    public static void showSettingsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage("Notification permission is required for scheduled focus sessions to work properly.\n\n" +
                           "Please enable notifications in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openAppSettings(activity);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Open app settings for manual permission enabling
     */
    private static void openAppSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open app settings", e);
            // Fallback to general settings
            try {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                activity.startActivity(intent);
            } catch (Exception e2) {
                Log.e(TAG, "Failed to open notification settings", e2);
            }
        }
    }
    
    /**
     * Check if notification permission is permanently denied
     */
    public static boolean isPermissionPermanentlyDenied(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return !hasNotificationPermission(activity) && 
                   !shouldShowPermissionRationale(activity);
        }
        return false;
    }
    
    /**
     * Get user-friendly permission status message
     */
    public static String getPermissionStatusMessage(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return "Notifications are enabled by default";
        }
        
        if (hasNotificationPermission(context)) {
            return "Notification permission granted";
        } else {
            return "Notification permission required for scheduled sessions";
        }
    }
} 