package com.grepguru.zenlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * Foreground Service to handle scheduled focus sessions
 * Uses setFullScreenIntent to automatically launch LockScreenActivity from background
 * Bypasses Android 12+ background activity launch restrictions
 */
public class LockScreenService extends Service {
    
    private static final String TAG = "LockScreenService";
    private static final String CHANNEL_ID = "LOCK_SCREEN_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    
    // Intent extras
    public static final String EXTRA_SCHEDULE_NAME = "schedule_name";
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_DURATION_MINUTES = "duration_minutes";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LockScreenService created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LockScreenService started");
        
        if (intent != null) {
            String scheduleName = intent.getStringExtra(EXTRA_SCHEDULE_NAME);
            int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1);
            int durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0);
            
            Log.d(TAG, "Starting focus session: " + scheduleName + " (" + durationMinutes + " min)");
            
            try {
                // Create notification channel
                createNotificationChannel();
                
                // Start foreground service with notification
                startForeground(NOTIFICATION_ID, createFocusSessionNotification(scheduleName, scheduleId, durationMinutes));
                Log.d(TAG, "Foreground service started successfully");
                
                // Launch LockScreenActivity using full screen intent
                launchLockScreenActivity(scheduleName, scheduleId, durationMinutes);
                
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException starting foreground service: " + e.getMessage());
                // Fallback: try to launch activity directly
                launchLockScreenActivity(scheduleName, scheduleId, durationMinutes);
            } catch (Exception e) {
                Log.e(TAG, "Error in LockScreenService: " + e.getMessage(), e);
                // Fallback: try to launch activity directly
                launchLockScreenActivity(scheduleName, scheduleId, durationMinutes);
            }
        }
        
        // Stop service after launching activity
        stopSelf();
        return START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LockScreenService destroyed");
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Focus Session Service",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Service for managing scheduled focus sessions");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Create notification with full screen intent to launch LockScreenActivity
     */
    private Notification createFocusSessionNotification(String scheduleName, int scheduleId, int durationMinutes) {
        // Create intent to launch LockScreenActivity
        Intent lockIntent = new Intent(this, LockScreenActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        lockIntent.putExtra("from_schedule", true);
        lockIntent.putExtra("schedule_name", scheduleName);
        lockIntent.putExtra("schedule_id", scheduleId);
        lockIntent.putExtra("lockDuration", durationMinutes * 60 * 1000L);
        
        // Create pending intent for full screen intent
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            scheduleId, // Use schedule ID as unique request code
            lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Create regular pending intent for notification tap
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
            this,
            scheduleId + 1000, // Different request code
            lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Focus Session Active")
            .setContentText(scheduleName + " - " + durationMinutes + " minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) // This automatically launches the activity
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        return builder.build();
    }
    
    /**
     * Launch LockScreenActivity directly (fallback if full screen intent fails)
     */
    private void launchLockScreenActivity(String scheduleName, int scheduleId, int durationMinutes) {
        try {
            Intent lockIntent = new Intent(this, LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            lockIntent.putExtra("from_schedule", true);
            lockIntent.putExtra("schedule_name", scheduleName);
            lockIntent.putExtra("schedule_id", scheduleId);
            lockIntent.putExtra("lockDuration", durationMinutes * 60 * 1000L);
            
            startActivity(lockIntent);
            Log.d(TAG, "LockScreenActivity launched directly for: " + scheduleName);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch LockScreenActivity directly", e);
        }
    }
}
