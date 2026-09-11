package com.grepguru.zenlock.utils;

import java.util.Calendar;
import com.grepguru.zenlock.model.ScheduleModel;

/** Calendar-only alarm calculation, independently testable from Android services. */
public final class ScheduleTimes {
    private ScheduleTimes() {}
    public static Calendar next(ScheduleModel schedule, Calendar now) {
        Calendar triggerTime = (Calendar) now.clone();
        
        // Set the time
        triggerTime.set(Calendar.HOUR_OF_DAY, schedule.getStartHour());
        triggerTime.set(Calendar.MINUTE, schedule.getStartMinute());
        triggerTime.set(Calendar.SECOND, 0);
        triggerTime.set(Calendar.MILLISECOND, 0);
        
        
        switch (schedule.getRepeatType()) {
            case ONCE:
                // For one-time schedules, if time has passed today, return null
                // But if time is still available today, schedule for today
                if (!triggerTime.after(now)) {
                    return null;
                }
                return triggerTime;
                
            case DAILY:
                // If time has passed today, move to tomorrow
                if (!triggerTime.after(now)) {
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
    private static Calendar getNextWeeklyTriggerTime(ScheduleModel schedule, Calendar now, Calendar triggerTime) {
        java.util.Set<Integer> repeatDays = schedule.getRepeatDays();
        if (repeatDays.isEmpty()) {
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
    
}
