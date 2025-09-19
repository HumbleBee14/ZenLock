package com.grepguru.zenlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * PreNotificationReceiver - Handles pre-notifications for scheduled focus sessions
 * Shows notification X minutes before a scheduled focus session starts
 */
public class PreNotificationReceiver extends BroadcastReceiver {
    
    private static final String TAG = "PreNotificationReceiver";
    private static final String CHANNEL_ID = "zenlock_pre_notifications";
    private static final int NOTIFICATION_ID_BASE = 2000; // Base ID for pre-notifications
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Pre-notification received");
        
        // Get schedule details from intent
        int scheduleId = intent.getIntExtra("schedule_id", -1);
        String scheduleName = intent.getStringExtra("schedule_name");
        int durationMinutes = intent.getIntExtra("duration_minutes", 0);
        int preNotifyMinutes = intent.getIntExtra("pre_notify_minutes", 5);
        
        if (scheduleId == -1 || scheduleName == null || durationMinutes <= 0) {
            Log.e(TAG, "Invalid pre-notification data received");
            return;
        }
        
        Log.d(TAG, "Showing pre-notification for: " + scheduleName + " (starts in " + preNotifyMinutes + " minutes)");
        
        // Create notification channel
        createNotificationChannel(context);
        
        // Create intent to open the app
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Format duration for display
        String durationText = formatDuration(durationMinutes);
        
        // Build clean, minimal notification
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("Focus Session Starting Soon")
            .setContentText("Starting in " + preNotifyMinutes + " min (" + durationText + " long session)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(null) // Silent notification
            .setVibrate(new long[]{0, 200, 100, 200}) // Gentle vibration pattern
            .build();
        
        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + scheduleId, notification);
            Log.d(TAG, "Pre-notification shown for: " + scheduleName);
        }
    }
    
    /**
     * Format duration for display (e.g., "2h 30m" or "45m")
     */
    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        
        if (hours > 0) {
            return hours + "h " + mins + "m";
        } else {
            return mins + "m";
        }
    }
    
    /**
     * Create notification channel for pre-notifications
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Focus Session Pre-Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications before scheduled focus sessions start");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
