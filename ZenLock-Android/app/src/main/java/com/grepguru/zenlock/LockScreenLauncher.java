package com.grepguru.zenlock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * Alternative launcher for LockScreenActivity that doesn't require foreground service
 * Uses high-priority notification with full screen intent as a fallback approach
 */
public class LockScreenLauncher {
    
    private static final String TAG = "LockScreenLauncher";
    private static final String CHANNEL_ID = "LOCK_SCREEN_LAUNCHER_CHANNEL";
    
    /**
     * Launch LockScreenActivity using notification with full screen intent
     * This is a fallback when foreground service fails
     */
    public static void launchWithNotification(Context context, String scheduleName, int scheduleId, int durationMinutes) {
        try {
            Log.d(TAG, "Launching lock screen with notification for: " + scheduleName);
            
            // Create notification channel
            createNotificationChannel(context);
            
            // Create intent to launch LockScreenActivity
            Intent lockIntent = new Intent(context, LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            lockIntent.putExtra("from_schedule", true);
            lockIntent.putExtra("schedule_name", scheduleName);
            lockIntent.putExtra("schedule_id", scheduleId);
            lockIntent.putExtra("lockDuration", durationMinutes * 60 * 1000L);
            
            // Create pending intent for full screen intent
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                scheduleId, // Use schedule ID as unique request code
                lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Create regular pending intent for notification tap
            PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                scheduleId + 1000, // Different request code
                lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            boolean fullScreenAllowed = notificationManager != null
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Focus Session Started")
                .setContentText("Tap to open " + scheduleName + " (" + durationMinutes + " min)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(fullScreenAllowed)
                .setOngoing(!fullScreenAllowed)
                .setContentIntent(contentPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.notify(scheduleId, builder.build());
                Log.d(TAG, "Focus session notification shown for: " + scheduleName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch with notification", e);
            
            // Final fallback: try direct launch
            try {
                Intent lockIntent = new Intent(context, LockScreenActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                lockIntent.putExtra("from_schedule", true);
                lockIntent.putExtra("schedule_name", scheduleName);
                lockIntent.putExtra("schedule_id", scheduleId);
                lockIntent.putExtra("lockDuration", durationMinutes * 60 * 1000L);
                
                context.startActivity(lockIntent);
                Log.d(TAG, "LockScreenActivity launched directly as final fallback for: " + scheduleName);
            } catch (Exception directException) {
                Log.e(TAG, "Failed to launch LockScreenActivity directly as final fallback", directException);
            }
        }
    }
    
    /**
     * Launch LockScreenActivity from AppBlockerService when direct startActivity() is blocked.
     * Used as fallback on MIUI/HyperOS devices where background activity starts are silently dropped.
     * Uses full-screen intent notification which MIUI does NOT block.
     */
    private static final int BLOCKER_NOTIFICATION_ID = 9999;

    public static void launchFromBlocker(Context context) {
        try {
            Log.d(TAG, "Launching lock screen via notification fallback (blocker)");

            createNotificationChannel(context);

            Intent lockIntent = new Intent(context, LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                BLOCKER_NOTIFICATION_ID,
                lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                BLOCKER_NOTIFICATION_ID + 1,
                lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            boolean fullScreenAllowed = notificationManager != null
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("ZenLock Active")
                .setContentText(fullScreenAllowed ? "Focus session in progress" : "Tap to return to your focus session")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(!fullScreenAllowed)
                .setContentIntent(contentPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.notify(BLOCKER_NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Blocker notification shown, fullScreenAllowed=" + fullScreenAllowed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch blocker notification fallback", e);
        }
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Focus Session Launcher",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for launching focus sessions");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
