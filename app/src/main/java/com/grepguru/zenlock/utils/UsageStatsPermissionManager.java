package com.grepguru.zenlock.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

/**
 * UsageStatsPermissionManager - Manages usage stats permission
 * Handles checking and requesting usage access permission
 */
public class UsageStatsPermissionManager {
    
    private static final String TAG = "UsageStatsPermissionManager";
    private static final String PREFS_NAME = "UsageStatsPermissionPrefs";
    private static final String KEY_PERMISSION_REQUESTED = "permission_requested";
    private static final String KEY_PERMISSION_DENIED_COUNT = "permission_denied_count";
    
    /**
     * Check if the app has usage stats permission
     */
    public static boolean hasUsageStatsPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false; // UsageStatsManager not available before Lollipop
        }
        
        try {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager == null) return false;
            
            int mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (API 29) and above, use unsafeCheckOpNoThrow
                mode = appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName()
                );
            } else {
                // For versions below Q, use the deprecated method with suppression
                @SuppressWarnings("deprecation")
                int deprecatedMode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName()
                );
                mode = deprecatedMode;
            }
            
            boolean hasPermission = mode == AppOpsManager.MODE_ALLOWED;
            Log.d(TAG, "Usage stats permission status: " + hasPermission);
            return hasPermission;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking usage stats permission", e);
            return false;
        }
    }
    
    /**
     * Check if we should show permission request (haven't asked or user might have changed mind)
     */
    public static boolean shouldShowPermissionRequest(Context context) {
        if (hasUsageStatsPermission(context)) {
            return false; // Already granted
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int deniedCount = prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0);
        
        // Show request if:
        // 1. Never asked before, OR
        // 2. Denied less than 3 times (don't be too pushy)
        return deniedCount < 3;
    }
    
    /**
     * Open settings page for granting usage access permission
     */
    public static void requestUsageStatsPermission(Context context) {
        try {
            // Mark that we've requested permission
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putBoolean(KEY_PERMISSION_REQUESTED, true)
                .apply();
            
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened usage access settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening usage access settings", e);
        }
    }
    
    /**
     * Mark that user has seen the permission request but didn't grant it
     */
    public static void markPermissionDenied(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentCount = prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0);
        prefs.edit()
            .putInt(KEY_PERMISSION_DENIED_COUNT, currentCount + 1)
            .apply();
        Log.d(TAG, "Permission denied count: " + (currentCount + 1));
    }
    
    /**
     * Reset permission request state (for testing or when user grants permission)
     */
    public static void resetPermissionState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "Permission state reset");
    }
    
    /**
     * Check if usage stats is supported on this device
     */
    public static boolean isUsageStatsSupported() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }
    
    /**
     * Get user-friendly explanation for usage stats permission
     */
    public static String getPermissionExplanation() {
        return "ZenLock needs access to usage statistics to show you how much screen time you're saving. " +
               "This permission allows the app to see which apps you use and for how long, but this data " +
               "stays private on your device and is only used to calculate your focus statistics.";
    }
    
    /**
     * Get instructions for granting permission
     */
    public static String getPermissionInstructions() {
        return "To enable screen time tracking:\n\n" +
               "1. Tap 'Grant Permission' below\n" +
               "2. Find 'ZenLock' in the list\n" +
               "3. Toggle 'Permit usage access' ON\n" +
               "4. Return to the app\n\n" +
               "This helps ZenLock show you accurate focus statistics and time saved.";
    }
}
