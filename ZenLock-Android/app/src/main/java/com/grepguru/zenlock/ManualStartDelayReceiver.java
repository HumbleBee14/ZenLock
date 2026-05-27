package com.grepguru.zenlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.ManualStartDelayScheduler;

/**
 * Starts a manual focus session after a one-off home screen delay.
 */
public class ManualStartDelayReceiver extends BroadcastReceiver {

    private static final String TAG = "ManualStartReceiver";
    public static final String EXTRA_DURATION_MS = "duration_ms";
    public static final String EXTRA_ABSOLUTE_END_TIME_MS = "absolute_end_time_ms";

    @Override
    public void onReceive(Context context, Intent intent) {
        ManualStartDelayScheduler.clearPendingSessionState(context);

        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean isCurrentlyLocked = prefs.getBoolean("isLocked", false);
        long currentTime = System.currentTimeMillis();
        long currentLockEndTime = prefs.getLong("lockEndTime", 0L);

        if (isCurrentlyLocked && currentLockEndTime > 0 && currentTime >= currentLockEndTime) {
            prefs.edit()
                    .putBoolean("isLocked", false)
                    .remove("lockEndTime")
                    .remove("uptimeAtLock")
                    .remove("wasDeviceRestarted")
                    .remove("current_session_source")
                    .apply();
            isCurrentlyLocked = false;
        }

        if (isCurrentlyLocked) {
            Log.w(TAG, "A focus session is already active, skipping delayed manual session");
            return;
        }

        long durationMillis = intent.getLongExtra(EXTRA_DURATION_MS, 0L);
        long absoluteEndTimeMillis = intent.getLongExtra(EXTRA_ABSOLUTE_END_TIME_MS, 0L);

        if (absoluteEndTimeMillis > 0L) {
            durationMillis = absoluteEndTimeMillis - currentTime;
        }

        if (durationMillis <= 0L) {
            Log.w(TAG, "Delayed manual session expired before it could start");
            return;
        }

        long lockStartTime = currentTime;
        long lockEndTime = lockStartTime + durationMillis;
        long uptimeAtLock = android.os.SystemClock.elapsedRealtime();

        boolean saved = prefs.edit()
                .putBoolean("isLocked", true)
                .putLong("lockStartTime", lockStartTime)
                .putLong("lockEndTime", lockEndTime)
                .putLong("lockTargetDuration", durationMillis)
                .putLong("uptimeAtLock", uptimeAtLock)
                .putBoolean("wasDeviceRestarted", false)
                .putString("current_session_source", "manual")
                .commit();

        if (!saved) {
            Log.e(TAG, "Failed to save delayed manual focus session state");
            return;
        }

        AnalyticsManager analyticsManager = new AnalyticsManager(context);
        analyticsManager.startSession(durationMillis, "manual");

        int durationMinutes = (int) Math.max(1L, durationMillis / 60_000L);
        Intent serviceIntent = new Intent(context, LockScreenService.class);
        serviceIntent.putExtra(LockScreenService.EXTRA_SCHEDULE_NAME, "Focus Session");
        serviceIntent.putExtra(LockScreenService.EXTRA_SCHEDULE_ID, 41041);
        serviceIntent.putExtra(LockScreenService.EXTRA_DURATION_MINUTES, durationMinutes);
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(
                        context.getApplicationContext(),
                        "Focus session starting now",
                        Toast.LENGTH_SHORT
                ).show()
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Started delayed manual focus session");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch delayed manual focus session via service", e);
            LockScreenLauncher.launchWithNotification(context, "Focus Session", 41041, durationMinutes);
        }
    }
}
