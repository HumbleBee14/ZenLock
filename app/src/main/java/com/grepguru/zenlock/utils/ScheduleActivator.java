package com.grepguru.zenlock.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.grepguru.zenlock.LockScreenActivity;
import com.grepguru.zenlock.model.ScheduleModel;

import java.util.Calendar;
import java.util.List;

/**
 * Simple schedule activator using AlarmManager
 * Handles scheduling focus sessions at specified times
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
                Log.d(TAG, "Schedule " + schedule.getName() + " has no next trigger time");
                return;
            }
            
            // Create intent for lock screen
            Intent intent = new Intent(context, LockScreenActivity.class);
            intent.putExtra("schedule_id", schedule.getId());
            intent.putExtra("duration_minutes", schedule.getFocusDurationMinutes());
            intent.putExtra("from_schedule", true);
            intent.putExtra("schedule_name", schedule.getName());
            
            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Set alarm
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime.getTimeInMillis(),
                    pendingIntent
                );
                
                Log.d(TAG, "Scheduled " + schedule.getName() + " for " + 
                      String.format("%02d:%02d", triggerTime.get(Calendar.HOUR_OF_DAY), 
                                  triggerTime.get(Calendar.MINUTE)));
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
            Intent intent = new Intent(context, LockScreenActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
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
        
        // If time has passed today, move to next occurrence
        if (triggerTime.before(now)) {
            switch (schedule.getRepeatType()) {
                case ONCE:
                    // One-time schedule has passed
                    return null;
                    
                case DAILY:
                    // Move to tomorrow
                    triggerTime.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                    
                case WEEKLY:
                    // Find next valid day this week or next week
                    int currentDay = now.get(Calendar.DAY_OF_WEEK);
                    int nextDay = findNextValidDay(schedule.getRepeatDays(), currentDay);
                    
                    if (nextDay == -1) {
                        // No valid days this week, move to next week
                        triggerTime.add(Calendar.WEEK_OF_YEAR, 1);
                        nextDay = findNextValidDay(schedule.getRepeatDays(), 0);
                    }
                    
                    triggerTime.set(Calendar.DAY_OF_WEEK, nextDay);
                    break;
            }
        }
        
        return triggerTime;
    }
    
    /**
     * Find the next valid day of the week
     */
    private int findNextValidDay(java.util.Set<Integer> repeatDays, int startDay) {
        for (int day = startDay + 1; day <= Calendar.SATURDAY; day++) {
            if (repeatDays.contains(day)) {
                return day;
            }
        }
        return -1; // No valid day found
    }
    
    /**
     * Reschedule all schedules (useful after device reboot)
     */
    public void rescheduleAllSchedules() {
        Log.d(TAG, "Rescheduling all schedules");
        scheduleAllSchedules();
    }
} 