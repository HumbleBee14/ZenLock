package com.grepguru.zenlock.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.grepguru.zenlock.ScheduleTriggerReceiver;
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
            
            // Check permission before setting alarm
            if (!AlarmPermissionManager.canScheduleExactAlarms(context)) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted");
                return;
            }
            
            // Set exact alarm
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }
            
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime.getTimeInMillis(),
                    pendingIntent
                );
                
                Log.d(TAG, "Scheduled " + schedule.getName() + " for " + 
                      String.format("%02d:%02d on %s", 
                                  triggerTime.get(Calendar.HOUR_OF_DAY), 
                                  triggerTime.get(Calendar.MINUTE),
                                  formatDate(triggerTime)));
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when setting alarm - permission issue", e);
                return;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule " + schedule.getName(), e);
        }
    }
    
    /**
     * Cancel a specific schedule
     */
    public void cancelSchedule(ScheduleModel schedule) {
        try {
            Intent intent = new Intent(context, ScheduleTriggerReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Cancelled schedule: " + schedule.getName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel schedule " + schedule.getName(), e);
        }
    }
    
    /**
     * Calculate the next trigger time for a schedule
     */
    private Calendar getNextTriggerTime(ScheduleModel schedule) {
        Calendar now = Calendar.getInstance();
        Calendar triggerTime = Calendar.getInstance();
        
        // Set the time
        triggerTime.set(Calendar.HOUR_OF_DAY, schedule.getStartHour());
        triggerTime.set(Calendar.MINUTE, schedule.getStartMinute());
        triggerTime.set(Calendar.SECOND, 0);
        triggerTime.set(Calendar.MILLISECOND, 0);
        
        
        switch (schedule.getRepeatType()) {
            case ONCE:
                // For one-time schedules, if time has passed today, return null
                // But if time is still available today, schedule for today
                if (triggerTime.before(now)) {
                    return null;
                }
                return triggerTime;
                
            case DAILY:
                // If time has passed today, move to tomorrow
                if (triggerTime.before(now)) {
                    triggerTime.add(Calendar.DAY_OF_YEAR, 1);
                }
                return triggerTime;
                
            case WEEKLY:
                return getNextWeeklyTriggerTime(schedule, now, triggerTime);
                
            default:
                return null;
        }
    }
    
    /**
     * Calculate next trigger time for weekly schedules
     */
    private Calendar getNextWeeklyTriggerTime(ScheduleModel schedule, Calendar now, Calendar triggerTime) {
        java.util.Set<Integer> repeatDays = schedule.getRepeatDays();
        if (repeatDays.isEmpty()) {
            Log.w(TAG, "Weekly schedule has no repeat days set");
            return null;
        }
        
        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        
        // Check if schedule can run today
        if (repeatDays.contains(currentDayOfWeek) && triggerTime.after(now)) {
            return triggerTime;
        }
        
        // Find next valid day
        Calendar nextTrigger = (Calendar) triggerTime.clone();
        
        // Check remaining days this week
        for (int daysToAdd = 1; daysToAdd <= 7; daysToAdd++) {
            nextTrigger.add(Calendar.DAY_OF_YEAR, 1);
            int dayOfWeek = nextTrigger.get(Calendar.DAY_OF_WEEK);
            
            if (repeatDays.contains(dayOfWeek)) {
                return nextTrigger;
            }
        }
        
        // Should not reach here, but fallback
        return null;
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