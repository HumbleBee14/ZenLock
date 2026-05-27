package com.grepguru.zenlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.grepguru.zenlock.model.ScheduleModel;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.ScheduleManager;

/**
 * ScheduleTriggerReceiver - Handles scheduled focus session activation
 * Triggered by AlarmManager when a scheduled focus session should start
 * Properly sets up the focus session state and launches LockScreenActivity
 */
public class ScheduleTriggerReceiver extends BroadcastReceiver {
    
    private static final String TAG = "ScheduleTriggerReceiver";
    
    // Intent extras
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_SCHEDULE_NAME = "schedule_name";
    public static final String EXTRA_DURATION_MINUTES = "duration_minutes";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Schedule trigger received");
        
        // Get schedule details from intent
        int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1);
        String scheduleName = intent.getStringExtra(EXTRA_SCHEDULE_NAME);
        int durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0);
        
        if (scheduleId == -1 || durationMinutes <= 0) {
            Log.e(TAG, "Invalid schedule data received");
            return;
        }
        
        Log.d(TAG, "Starting scheduled focus session: " + scheduleName + " (" + durationMinutes + " minutes)");
        
        // Clear any pre-notification for this schedule
        clearPreNotification(context, scheduleId);
        
        // Check if there's already an active session
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean isCurrentlyLocked = prefs.getBoolean("isLocked", false);
        long lockEndTime = prefs.getLong("lockEndTime", 0);
        long currentTime = System.currentTimeMillis();
        
                // Check if session is actually expired
                if (isCurrentlyLocked && lockEndTime > 0 && currentTime >= lockEndTime) {
                    Log.w(TAG, "Found expired session, cleaning up stale state");
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isLocked", false);
                    editor.remove("lockEndTime");
                    editor.remove("uptimeAtLock");
                    editor.remove("wasDeviceRestarted");
                    editor.remove("current_session_source");
                    editor.apply();

                    isCurrentlyLocked = false;
                }
        
        if (isCurrentlyLocked) {
            Log.w(TAG, "Focus session already active, skipping scheduled session");
            return;
        }
        
        // Verify schedule still exists and is enabled
        ScheduleManager scheduleManager = new ScheduleManager(context);
        ScheduleModel schedule = scheduleManager.getScheduleById(scheduleId);
        
        if (schedule == null || !schedule.isEnabled()) {
            Log.w(TAG, "Schedule no longer exists or is disabled, skipping");
            return;
        }
        
        // Set up focus session state (same as HomeFragment does)
        boolean setupSuccess = setupFocusSession(context, durationMinutes, scheduleName);
        
        if (!setupSuccess) {
            Log.e(TAG, "Failed to setup focus session state");
            return;
        }
        
        // Start LockScreenService (Foreground Service) to handle background launch
        Intent serviceIntent = new Intent(context, LockScreenService.class);
        serviceIntent.putExtra(LockScreenService.EXTRA_SCHEDULE_NAME, scheduleName);
        serviceIntent.putExtra(LockScreenService.EXTRA_SCHEDULE_ID, scheduleId);
        serviceIntent.putExtra(LockScreenService.EXTRA_DURATION_MINUTES, durationMinutes);
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "LockScreenService started for scheduled session: " + scheduleName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LockScreenService, trying notification launcher", e);
            
            // Fallback: use notification-based launcher
            LockScreenLauncher.launchWithNotification(context, scheduleName, scheduleId, durationMinutes);
        }
        
        // Reschedule for next occurrence (if recurring)
        rescheduleIfNeeded(context, schedule);
    }
    
    /**
     * Set up focus session state in SharedPreferences (mimics HomeFragment behavior)
     */
    private boolean setupFocusSession(Context context, int durationMinutes, String scheduleName) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
            
            long lockDurationMillis = durationMinutes * 60 * 1000L;
            long lockStartTime = System.currentTimeMillis();
            long lockEndTime = lockStartTime + lockDurationMillis;
            long uptimeAtLock = android.os.SystemClock.elapsedRealtime();
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isLocked", true);
            editor.putLong("lockStartTime", lockStartTime);
            editor.putLong("lockEndTime", lockEndTime);
            editor.putLong("lockTargetDuration", lockDurationMillis);
            editor.putLong("uptimeAtLock", uptimeAtLock);  // Critical for restart detection
            editor.putBoolean("wasDeviceRestarted", false);
            editor.putString("current_session_source", "schedule:" + scheduleName);
            
            boolean commitResult = editor.commit(); // Use commit() for immediate write
            
            if (!commitResult) {
                Log.e(TAG, "Failed to save focus session state");
                return false;
            }
            
            // Start analytics tracking
            AnalyticsManager analyticsManager = new AnalyticsManager(context);
            analyticsManager.startSession(lockDurationMillis);
            
            Log.d(TAG, "Focus session state setup complete for scheduled session");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup focus session state", e);
            return false;
        }
    }
    
    /**
     * Clear pre-notification for a specific schedule
     */
    private void clearPreNotification(Context context, int scheduleId) {
        try {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                // Clear pre-notification using the same ID used in PreNotificationReceiver
                int preNotificationId = 2000 + scheduleId; // Base ID + schedule ID
                notificationManager.cancel(preNotificationId);
                Log.d(TAG, "Cleared pre-notification for schedule ID: " + scheduleId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear pre-notification for schedule ID: " + scheduleId, e);
        }
    }
    
    /**
     * Reschedule recurring schedules for next occurrence
     */
    private void rescheduleIfNeeded(Context context, ScheduleModel schedule) {
        if (schedule.getRepeatType() != ScheduleModel.RepeatType.ONCE) {
            // For recurring schedules, reschedule for next occurrence
            com.grepguru.zenlock.utils.ScheduleActivator activator = 
                new com.grepguru.zenlock.utils.ScheduleActivator(context);
            activator.scheduleSchedule(schedule);
            Log.d(TAG, "Rescheduled recurring schedule: " + schedule.getName());
        } else {
            // For one-time schedules, disable them after execution
            ScheduleManager scheduleManager = new ScheduleManager(context);
            schedule.setEnabled(false);
            scheduleManager.updateSchedule(schedule);
            Log.d(TAG, "Disabled one-time schedule: " + schedule.getName());
        }
    }
}
