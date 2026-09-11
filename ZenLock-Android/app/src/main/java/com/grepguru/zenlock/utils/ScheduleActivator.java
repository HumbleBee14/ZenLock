package com.grepguru.zenlock.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.grepguru.zenlock.ScheduleTriggerReceiver;
import com.grepguru.zenlock.PreNotificationReceiver;
import com.grepguru.zenlock.model.ScheduleModel;
import com.grepguru.zenlock.utils.AlarmPermissionManager;

import java.util.Calendar;
import java.util.List;

/**
 * Enhanced ScheduleActivator using AlarmManager with proper focus session integration
 * Handles scheduling focus sessions at specified times using ScheduleTriggerReceiver
 * Properly integrates with existing focus session flow and state management
 */
public class ScheduleActivator {
    
    private static final String TAG = "ScheduleActivator";
    private final Context context;
    private final ScheduleManager scheduleManager;
    private final AlarmManager alarmManager;
    
    public ScheduleActivator(Context context) {
        this.context = context;
        this.scheduleManager = new ScheduleManager(context);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Schedule all enabled schedules
     */
    public void scheduleAllSchedules() {
        List<ScheduleModel> enabledSchedules = scheduleManager.getEnabledSchedules();
        Log.d(TAG, "Scheduling " + enabledSchedules.size() + " enabled schedules");
        
        for (ScheduleModel schedule : enabledSchedules) {
            scheduleSchedule(schedule);
        }
    }
    
    /**
     * Schedule a specific schedule
     */
    public void scheduleSchedule(ScheduleModel schedule) {
        // Replace both alarms, including a reminder removed or moved by an edit.
        cancelSchedule(schedule);
        if (!schedule.isEnabled()) {
            Log.d(TAG, "Schedule " + schedule.getName() + " is disabled, skipping");
            return;
        }
        
        try {
            // Calculate next trigger time
            Calendar triggerTime = getNextTriggerTime(schedule);
            if (triggerTime == null) {
                Log.w(TAG, "Schedule " + schedule.getName() + " has no next trigger time");
                return;
            }
            
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }
            
            // Schedule main trigger
            scheduleMainTrigger(schedule, triggerTime);
            
            // Schedule pre-notification if enabled
            if (schedule.isPreNotifyEnabled() && schedule.getPreNotifyMinutes() > 0) {
                schedulePreNotification(schedule, triggerTime);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule " + schedule.getName(), e);
        }
    }
    
    /**
     * Schedule the main focus session trigger
     */
    private void scheduleMainTrigger(ScheduleModel schedule, Calendar triggerTime) {
        try {
            // Create intent for ScheduleTriggerReceiver
            Intent intent = new Intent(context, ScheduleTriggerReceiver.class);
            intent.putExtra(ScheduleTriggerReceiver.EXTRA_SCHEDULE_ID, schedule.getId());
            intent.putExtra(ScheduleTriggerReceiver.EXTRA_SCHEDULE_NAME, schedule.getName());
            intent.putExtra(ScheduleTriggerReceiver.EXTRA_DURATION_MINUTES, schedule.getFocusDurationMinutes());
            
            // Create pending intent for BroadcastReceiver
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            setAlarm(triggerTime.getTimeInMillis(), pendingIntent);

            Log.d(TAG, "Scheduled " + schedule.getName() + " for " +
                  String.format("%02d:%02d on %s", 
                              triggerTime.get(Calendar.HOUR_OF_DAY), 
                              triggerTime.get(Calendar.MINUTE),
                              formatDate(triggerTime)));
                              
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when setting main trigger - permission issue", e);
        }
    }
    
    /**
     * Schedule pre-notification for the schedule
     */
    private void schedulePreNotification(ScheduleModel schedule, Calendar triggerTime) {
        try {
            // Calculate pre-notification time
            Calendar preNotifyTime = (Calendar) triggerTime.clone();
            preNotifyTime.add(Calendar.MINUTE, -schedule.getPreNotifyMinutes());
            
            // Don't schedule if pre-notification time has already passed
            if (preNotifyTime.before(Calendar.getInstance())) {
                Log.d(TAG, "Pre-notification time has passed for " + schedule.getName() + ", skipping");
                return;
            }
            
            // Create intent for PreNotificationReceiver
            Intent preNotifyIntent = new Intent(context, PreNotificationReceiver.class);
            preNotifyIntent.putExtra("schedule_id", schedule.getId());
            preNotifyIntent.putExtra("schedule_name", schedule.getName());
            preNotifyIntent.putExtra("duration_minutes", schedule.getFocusDurationMinutes());
            preNotifyIntent.putExtra("pre_notify_minutes", schedule.getPreNotifyMinutes());
            
            // Create pending intent for pre-notification (use negative ID to avoid conflicts)
            PendingIntent preNotifyPendingIntent = PendingIntent.getBroadcast(
                context,
                -schedule.getId(), // Negative ID to avoid conflicts with main trigger
                preNotifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            setAlarm(preNotifyTime.getTimeInMillis(), preNotifyPendingIntent);

            Log.d(TAG, "Scheduled pre-notification for " + schedule.getName() + " at " + 
                  String.format("%02d:%02d on %s", 
                              preNotifyTime.get(Calendar.HOUR_OF_DAY), 
                              preNotifyTime.get(Calendar.MINUTE),
                              formatDate(preNotifyTime)) + 
                  " (" + schedule.getPreNotifyMinutes() + " minutes before)");
                              
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when setting pre-notification - permission issue", e);
        }
    }
    
    private void setAlarm(long triggerAtMillis, PendingIntent pendingIntent) {
        if (AlarmPermissionManager.canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            );
        } else {
            Log.w(TAG, "Exact alarm permission not granted, falling back to windowed alarm");
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                10 * 60 * 1000L,
                pendingIntent
            );
        }
    }

    /**
     * Cancel a specific schedule
     */
    public void cancelSchedule(ScheduleModel schedule) {
        try {
            // Cancel main trigger
            Intent intent = new Intent(context, ScheduleTriggerReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Cancelled main trigger for: " + schedule.getName());
            }
            
            // Cancel pre-notification if it exists
            Intent preNotifyIntent = new Intent(context, PreNotificationReceiver.class);
            PendingIntent preNotifyPendingIntent = PendingIntent.getBroadcast(
                context,
                -schedule.getId(), // Negative ID to match the one used in scheduling
                preNotifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (alarmManager != null) {
                alarmManager.cancel(preNotifyPendingIntent);
                Log.d(TAG, "Cancelled pre-notification for: " + schedule.getName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel schedule " + schedule.getName(), e);
        }
    }
    
    /**
     * Calculate the next trigger time for a schedule
     */
    private Calendar getNextTriggerTime(ScheduleModel schedule) {
        return ScheduleTimes.next(schedule, Calendar.getInstance());
    }

    /**
     * Format date for logging
     */
    private String formatDate(Calendar calendar) {
        return String.format("%d/%d/%d", 
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.YEAR));
    }
    
    /**
     * Reschedule all schedules (useful after device reboot)
     */
    public void rescheduleAllSchedules() {
        Log.d(TAG, "Rescheduling all schedules");
        scheduleAllSchedules();
    }
    
    /**
     * Cancel all scheduled alarms (useful for cleanup)
     */
    public void cancelAllSchedules() {
        List<ScheduleModel> allSchedules = scheduleManager.getAllSchedules();
        for (ScheduleModel schedule : allSchedules) {
            cancelSchedule(schedule);
        }
        Log.d(TAG, "Cancelled all scheduled alarms");
    }
} 