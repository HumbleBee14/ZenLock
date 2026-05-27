package com.grepguru.zenlock.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.app.AlertDialog;

/**
 * Utility class to manage FOREGROUND_SERVICE permission
 * Required for starting foreground services on Android 14+
 */
public class ForegroundServicePermissionManager {
    
    private static final String TAG = "ForegroundServicePermissionManager";
    
    /**
     * Check if the app can start foreground services
     * @param context The context
     * @return True if foreground services can be started, false otherwise
     */
    public static boolean canStartForegroundService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34) and above
            // For Android 14+, we need to check if the app has the FOREGROUND_SERVICE permission
            // and if the user has granted the specific foreground service type permission
            boolean hasPermission = context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Can start foreground service (API " + Build.VERSION.SDK_INT + "): " + hasPermission);
            return hasPermission;
        } else {
            // For API < 34, FOREGROUND_SERVICE is granted by default if declared in manifest
            Log.d(TAG, "Foreground service implicitly allowed for API " + Build.VERSION.SDK_INT);
            return true;
        }
    }
    
    /**
     * Request the FOREGROUND_SERVICE permission by opening the system settings
     * This should be called from an Activity context
     * @param context The Activity context
     */
    public static void requestForegroundServicePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!canStartForegroundService(context)) {
                new AlertDialog.Builder(context)
                        .setTitle("Background Service Permission Required")
                        .setMessage("ZenLock needs permission to run background services for automatic focus sessions.\n\n" +
                                "This allows the app to automatically launch the lock screen when scheduled, even when your phone is locked or you're using other apps.\n\n" +
                                "Please grant 'Allow background activity' permission in the next screen.")
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                            context.startActivity(intent);
                            Log.d(TAG, "Opened app settings for foreground service permission request.");
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Log.w(TAG, "Foreground service permission request cancelled by user.");
                            // Optionally, inform the user that automatic lock might not work reliably
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }
}
