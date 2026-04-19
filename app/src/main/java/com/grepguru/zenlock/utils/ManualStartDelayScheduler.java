package com.grepguru.zenlock.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.grepguru.zenlock.MainActivity;
import com.grepguru.zenlock.ManualStartDelayReceiver;
import com.grepguru.zenlock.R;

/**
 * Schedules one-off delayed manual focus sessions from the home screen.
 */
public final class ManualStartDelayScheduler {

    private static final String TAG = "ManualStartDelay";
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final int REQUEST_CODE = 41041;
    private static final int NOTIFICATION_REQUEST_CODE = 41042;
    private static final int NOTIFICATION_ID = 41043;
    private static final String CHANNEL_ID = "manual_focus_schedule";

    private static final String PREF_PENDING = "pending_manual_start";
    private static final String PREF_START_AT = "pending_manual_start_at";
    private static final String PREF_DELAY_MINUTES = "pending_manual_delay_minutes";
    private static final String PREF_DURATION_MS = "pending_manual_duration_ms";
    private static final String PREF_END_AT_MS = "pending_manual_end_at_ms";

    private ManualStartDelayScheduler() {}

    public static boolean scheduleSession(Context context, int delayMinutes, long durationMillis,
                                          @Nullable Long absoluteEndTimeMillis) {
        if (delayMinutes <= 0) {
            cancelPendingSession(context);
            return false;
        }

        if (!AlarmPermissionManager.canScheduleExactAlarms(context)) {
            Log.w(TAG, "Exact alarm permission missing, cannot schedule delayed manual session");
            return false;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager unavailable");
            return false;
        }

        long triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60_000L);
        long absoluteEndMillis = absoluteEndTimeMillis != null ? absoluteEndTimeMillis : 0L;

        try {
            PendingIntent pendingIntent = createPendingIntent(
                    context, durationMillis, absoluteEndMillis
            );

            alarmManager.cancel(pendingIntent);
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );

            savePendingSessionState(
                    context,
                    triggerAtMillis,
                    delayMinutes,
                    durationMillis,
                    absoluteEndMillis
            );
            showScheduledNotification(context, triggerAtMillis);

            Log.d(TAG, "Scheduled delayed manual session for " + triggerAtMillis);
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "Unable to schedule delayed manual session", e);
            return false;
        }
    }

    public static void cancelPendingSession(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createPendingIntent(context, 0L, 0L));
        }
        clearPendingSessionState(context);
    }

    public static boolean hasPendingSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_PENDING, false)) {
            return false;
        }

        long triggerAtMillis = prefs.getLong(PREF_START_AT, 0L);
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Clearing stale delayed manual session state");
            clearPendingSessionState(context);
            return false;
        }

        return true;
    }

    public static long getPendingStartAtMillis(Context context) {
        if (!hasPendingSession(context)) {
            return 0L;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_START_AT, 0L);
    }

    public static int getPendingDelayMinutes(Context context) {
        if (!hasPendingSession(context)) {
            return 0;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_DELAY_MINUTES, 0);
    }

    public static void reschedulePendingSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_PENDING, false)) {
            return;
        }

        long triggerAtMillis = prefs.getLong(PREF_START_AT, 0L);
        long durationMillis = prefs.getLong(PREF_DURATION_MS, 0L);
        long absoluteEndMillis = prefs.getLong(PREF_END_AT_MS, 0L);
        int delayMinutes = prefs.getInt(PREF_DELAY_MINUTES, 0);

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Pending delayed session has already expired after reboot");
            clearPendingSessionState(context);
            return;
        }

        if (!AlarmPermissionManager.canScheduleExactAlarms(context)) {
            Log.w(TAG, "Clearing delayed session after reboot because exact alarms are unavailable");
            clearPendingSessionState(context);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager unavailable during delayed session reschedule");
            return;
        }

        try {
            PendingIntent pendingIntent = createPendingIntent(context, durationMillis, absoluteEndMillis);
            alarmManager.cancel(pendingIntent);
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
            showScheduledNotification(context, triggerAtMillis);
            Log.d(TAG, "Rescheduled delayed manual session after reboot with original delay " + delayMinutes);
        } catch (SecurityException e) {
            Log.e(TAG, "Unable to reschedule delayed manual session after reboot", e);
        }
    }

    public static void clearPendingSessionState(Context context) {
        cancelScheduledNotification(context);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_PENDING)
                .remove(PREF_START_AT)
                .remove(PREF_DELAY_MINUTES)
                .remove(PREF_DURATION_MS)
                .remove(PREF_END_AT_MS)
                .apply();
    }

    private static PendingIntent createPendingIntent(Context context, long durationMillis, long absoluteEndMillis) {
        Intent intent = new Intent(context, ManualStartDelayReceiver.class);
        intent.putExtra(ManualStartDelayReceiver.EXTRA_DURATION_MS, durationMillis);
        intent.putExtra(ManualStartDelayReceiver.EXTRA_ABSOLUTE_END_TIME_MS, absoluteEndMillis);

        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void savePendingSessionState(Context context, long triggerAtMillis, int delayMinutes,
                                                long durationMillis, long absoluteEndMillis) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_PENDING, true)
                .putLong(PREF_START_AT, triggerAtMillis)
                .putInt(PREF_DELAY_MINUTES, delayMinutes)
                .putLong(PREF_DURATION_MS, durationMillis)
                .putLong(PREF_END_AT_MS, absoluteEndMillis)
                .apply();
    }

    private static void showScheduledNotification(Context context, long triggerAtMillis) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return;
        }

        createNotificationChannel(context);

        int remainingMinutes = Math.max(1, (int) Math.ceil(
                Math.max(0L, triggerAtMillis - System.currentTimeMillis()) / 60_000d
        ));
        String contentText = remainingMinutes == 1
                ? "Starts in 1 minute"
                : "Starts in " + remainingMinutes + " minutes";

        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_REQUEST_CODE,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lock)
                .setContentTitle("Focus session scheduled")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private static void cancelScheduledNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Manual Focus Scheduling",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for delayed focus sessions");
            channel.enableVibration(false);
            channel.setShowBadge(false);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
