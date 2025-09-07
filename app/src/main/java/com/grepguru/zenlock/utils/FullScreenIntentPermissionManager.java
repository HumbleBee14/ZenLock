package com.grepguru.zenlock.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.app.AlertDialog;

import com.grepguru.zenlock.R;

/**
 * Utility class to manage USE_FULL_SCREEN_INTENT permission
 * Required for setFullScreenIntent to work properly on Android 14+
 */
public class FullScreenIntentPermissionManager {
    
    private static final String TAG = "FullScreenIntentPermissionManager";
    
    /**
     * Check if the app can use full screen intents
     * @param context The context
     * @return True if full screen intents can be used, false otherwise
     */
    public static boolean canUseFullScreenIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34) and above
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            boolean canUse = notificationManager.canUseFullScreenIntent();
            Log.d(TAG, "Can use full screen intent (API " + Build.VERSION.SDK_INT + "): " + canUse);
            return canUse;
        } else {
            // For API < 34, USE_FULL_SCREEN_INTENT is granted by default if declared in manifest
            Log.d(TAG, "Full screen intent implicitly allowed for API " + Build.VERSION.SDK_INT);
            return true;
        }
    }
    
    /**
     * Request the USE_FULL_SCREEN_INTENT permission by opening the system settings
     * This should be called from an Activity context
     * @param context The Activity context
     */
    public static void requestFullScreenIntentPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!canUseFullScreenIntent(context)) {
                new AlertDialog.Builder(context)
                        .setTitle("Full Screen Permission Required")
                        .setMessage("ZenLock needs permission to show full screen notifications for automatic focus sessions.\n\n" +
                                "This allows the app to launch the lock screen when scheduled.\n\n" +
                                "Please grant 'Display over other apps' permission in the next screen.")
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                            context.startActivity(intent);
                            Log.d(TAG, "Opened overlay permission settings for full screen intent request.");
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Log.w(TAG, "Full screen intent permission request cancelled by user.");
                            // Optionally, inform the user that automatic lock might not work reliably
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }
    
    /**
     * Check if the app has the necessary permissions for automatic focus sessions
     * @param context The context
     * @return True if all required permissions are granted
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        boolean hasNotificationPermission = NotificationPermissionManager.hasNotificationPermission(context);
        boolean hasExactAlarmPermission = AlarmPermissionManager.canScheduleExactAlarms(context);
        boolean hasFullScreenIntentPermission = canUseFullScreenIntent(context);
        
        Log.d(TAG, "Permission check:");
        Log.d(TAG, "  - Notification permission: " + hasNotificationPermission);
        Log.d(TAG, "  - Exact alarm permission: " + hasExactAlarmPermission);
        Log.d(TAG, "  - Full screen intent permission: " + hasFullScreenIntentPermission);
        
        return hasNotificationPermission && hasExactAlarmPermission && hasFullScreenIntentPermission;
    }
}
