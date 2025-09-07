package com.grepguru.zenlock.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.util.Log;

/**
 * Utility class for KeyguardManager operations to prevent code duplication
 * and ensure consistent behavior across the app
 */
public class KeyguardUtils {
    
    private static final String TAG = "KeyguardUtils";
    
    /**
     * Check if the system lock screen (Keyguard) is currently active
     * @param context The context
     * @return True if the system lock screen is active, false otherwise
     */
    public static boolean isKeyguardLocked(Context context) {
        try {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                boolean isLocked = keyguardManager.isKeyguardLocked();
                // Log.d(TAG, "System Keyguard is " + (isLocked ? "active" : "inactive"));
                return isLocked;
            } else {
                Log.w(TAG, "KeyguardManager is null, assuming keyguard is not locked");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking keyguard state", e);
            return false; // Assume not locked on error
        }
    }
    
    /**
     * Check if the system lock screen is active and log a message if it is
     * @param context The context
     * @param logMessage The message to log if keyguard is active
     * @return True if the system lock screen is active, false otherwise
     */
    public static boolean isKeyguardLockedWithLog(Context context, String logMessage) {
        boolean isLocked = isKeyguardLocked(context);
        if (isLocked && logMessage != null && !logMessage.isEmpty()) {
            Log.d(TAG, logMessage);
        }
        return isLocked;
    }
    
    /**
     * Check if the system lock screen is active and return early if it is
     * This is a convenience method for common patterns in the app
     * @param context The context
     * @param logMessage The message to log if keyguard is active
     * @return True if the system lock screen is active (should return early), false otherwise
     */
    public static boolean shouldReturnEarlyDueToKeyguard(Context context, String logMessage) {
        return isKeyguardLockedWithLog(context, logMessage);
    }
}