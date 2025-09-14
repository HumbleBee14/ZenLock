package com.grepguru.zenlock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class VibrationUtils {
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String VIBRATION_KEY = "vibration_enabled";

    public static void vibrate(Context context, long durationMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(VIBRATION_KEY, true); // Default: enabled
        Log.d("VibrationUtils", "Vibration enabled: " + enabled);
        if (!enabled) {
            Log.d("VibrationUtils", "Vibration is disabled in preferences.");
            return;
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            Log.d("VibrationUtils", "Vibrator service available. Triggering vibration for " + durationMs + "ms.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(durationMs);
            }
        } else {
            Log.d("VibrationUtils", "Vibrator service not available or device does not support vibration.");
        }
    }

    public static void debugVibration(Context context, long durationMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(VIBRATION_KEY, true); // Default: enabled
        Log.d("VibrationUtils", "Vibration enabled: " + enabled);
        if (!enabled) return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            Log.d("VibrationUtils", "Vibrator service available. Triggering vibration for " + durationMs + "ms.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
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
}
