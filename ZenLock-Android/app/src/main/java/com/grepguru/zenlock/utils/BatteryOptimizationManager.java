package com.grepguru.zenlock.utils;

import com.grepguru.zenlock.ui.AppLanguage;
import com.grepguru.zenlock.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * BatteryOptimizationManager - Handles the battery optimization exemption that
 * keeps scheduled alarms alive. Without it, aggressive OEMs (Samsung deep sleep,
 * MIUI, etc.) force-stop the app after a few days of no direct launches, which
 * cancels all pending alarms and breaks scheduled auto-lock.
 */
public class BatteryOptimizationManager {

    private static final String TAG = "BatteryOptManager";
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_LAST_PROMPT = "battery_exemption_last_prompt";
    private static final long PROMPT_INTERVAL_MS = 24 * 60 * 60 * 1000L;

    public static boolean isExempt(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static void requestExemption(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Direct exemption request unavailable, opening settings list", e);
            try {
                context.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e2) {
                Log.e(TAG, "Battery optimization settings unavailable", e2);
            }
        }
    }

    public static boolean isSamsungDevice() {
        return "samsung".equalsIgnoreCase(Build.MANUFACTURER);
    }

    /**
     * Shows the reliability prompt when schedules exist but the exemption is
     * missing. Safe to call anytime; no-op when already exempt.
     */
    public static void showScheduleReliabilityDialogIfNeeded(Context context) {
        if (isExempt(context)) {
            return;
        }

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0);
        long now = System.currentTimeMillis();
        if (now - lastPrompt < PROMPT_INTERVAL_MS) {
            return;
        }
        prefs.edit().putLong(KEY_LAST_PROMPT, now).apply();

        String message = AppLanguage.localizedContext(context).getString(R.string.background_to_make_sure_scheduled_sessions_start_on_time_every_day_zenlock_needs_to_be);

        if (isSamsungDevice()) {
            message += AppLanguage.localizedContext(context).getString(R.string.background_on_samsung_devices_also_check_settings_battery_background_usage_limits_and);
        }

        new AlertDialog.Builder(context)
                .setTitle(AppLanguage.localizedContext(context).getString(R.string.background_keep_schedules_running))
                .setMessage(message)
                .setPositiveButton(AppLanguage.localizedContext(context).getString(R.string.background_allow), (dialog, which) -> requestExemption(context))
                .setNegativeButton(AppLanguage.localizedContext(context).getString(R.string.background_not_now_677), null)
                .show();
    }
}
