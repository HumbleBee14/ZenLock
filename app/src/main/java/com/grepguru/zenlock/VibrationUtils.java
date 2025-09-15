package com.grepguru.zenlock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

public class VibrationUtils {
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String VIBRATION_KEY = "vibration_enabled";

    @SuppressWarnings("deprecation")
    public static void vibrate(Context context, long durationMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(VIBRATION_KEY, true); // Default: enabled
        Log.d("VibrationUtils", "Vibration enabled: " + enabled);
        if (!enabled) {
            Log.d("VibrationUtils", "Vibration is disabled in preferences.");
            return;
        }
        
        Vibrator vibrator = getVibrator(context);
        if (vibrator != null && vibrator.hasVibrator()) {
            Log.d("VibrationUtils", "Vibrator service available. Triggering vibration for " + durationMs + "ms.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // For backward compatibility with older APIs (below API 26)
                vibrator.vibrate(durationMs);
            }
        } else {
            Log.d("VibrationUtils", "Vibrator service not available or device does not support vibration.");
        }
    }

    @SuppressWarnings("deprecation")
    public static void debugVibration(Context context, long durationMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(VIBRATION_KEY, true); // Default: enabled
        Log.d("VibrationUtils", "Vibration enabled: " + enabled);
        if (!enabled) return;
        
        Vibrator vibrator = getVibrator(context);
        if (vibrator != null && vibrator.hasVibrator()) {
            Log.d("VibrationUtils", "Vibrator service available. Triggering vibration for " + durationMs + "ms.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // For backward compatibility with older APIs (below API 26)
                vibrator.vibrate(durationMs);
            }
        } else {
            Log.d("VibrationUtils", "Vibrator service not available.");
        }
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(VIBRATION_KEY, enabled).apply();
    }

    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(VIBRATION_KEY, true);
    }
    
    /**
     * Get Vibrator instance using modern APIs with backward compatibility
     */
    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ (Android 12+): Use VibratorManager
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            // API 30 and below: Use direct Vibrator service
            // Suppress deprecation warning as this is intentional for backward compatibility
            @SuppressWarnings("deprecation")
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            return vibrator;
        }
    }
}
