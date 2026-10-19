package com.grepguru.zenlock.model;

import com.grepguru.zenlock.ui.AppLanguage;
import com.grepguru.zenlock.R;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Model class for ZenLock schedules
 * Stores schedule configuration including timing, repeat patterns, and notifications
 */
public class ScheduleModel {
    
    // Schedule identification
    private int id;
    private String name;
    private boolean isEnabled;
    
    // Timing configuration
    private int startHour;
    private int startMinute;
    private int focusDurationMinutes; // Duration in minutes
    
    // Repeat configuration
    private RepeatType repeatType;
    private Set<Integer> repeatDays; // Calendar.DAY_OF_WEEK values (1=Sunday, 2=Monday, etc.)
    
    // Notification settings
    private boolean preNotifyEnabled;
    private int preNotifyMinutes; // Minutes before start time
    
    // Timestamps
    private long createdAt;
    private long lastModified;
    
    public enum RepeatType {
        ONCE(R.string.schedule_repeat_once),
        DAILY(R.string.schedule_repeat_daily),
        WEEKLY(R.string.schedule_repeat_weekly);
        
        private final int displayName;
        
        RepeatType(int displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName(android.content.Context context) {
            return AppLanguage.localizedContext(context).getString(displayName);
        }
    }
    
    // Constructor
    public ScheduleModel() {
        this.id = generateId();
        this.isEnabled = true;
        this.repeatType = RepeatType.DAILY;
        this.repeatDays = new HashSet<>();
        this.preNotifyEnabled = false;
        this.preNotifyMinutes = 5;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }
    
    // Generate unique ID
    private int generateId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { 
        this.isEnabled = enabled; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public int getStartHour() { return startHour; }
    public void setStartHour(int startHour) { 
        this.startHour = startHour; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public int getStartMinute() { return startMinute; }
    public void setStartMinute(int startMinute) { 
        this.startMinute = startMinute; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public int getFocusDurationMinutes() { return focusDurationMinutes; }
    public void setFocusDurationMinutes(int focusDurationMinutes) { 
        this.focusDurationMinutes = focusDurationMinutes; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public RepeatType getRepeatType() { return repeatType; }
    public void setRepeatType(RepeatType repeatType) { 
        this.repeatType = repeatType; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public Set<Integer> getRepeatDays() { return repeatDays; }
    public void setRepeatDays(Set<Integer> repeatDays) { 
        this.repeatDays = repeatDays; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public boolean isPreNotifyEnabled() { return preNotifyEnabled; }
    public void setPreNotifyEnabled(boolean preNotifyEnabled) { 
        this.preNotifyEnabled = preNotifyEnabled; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public int getPreNotifyMinutes() { return preNotifyMinutes; }
    public void setPreNotifyMinutes(int preNotifyMinutes) { 
        this.preNotifyMinutes = preNotifyMinutes; 
        this.lastModified = System.currentTimeMillis();
    }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    
    // Utility methods
    public String getFormattedStartTime(android.content.Context context) {
        return formatTime(context, startHour, startMinute);
    }

    public String getFormattedEndTime(android.content.Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMinute);
        calendar.add(Calendar.MINUTE, focusDurationMinutes);
        return formatTime(context, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    public String getFormattedDuration(android.content.Context context) {
        int hours = focusDurationMinutes / 60;
        int minutes = focusDurationMinutes % 60;
        if (hours > 0 && minutes > 0) return String.format(java.util.Locale.ROOT, AppLanguage.localizedContext(context).getString(R.string.background_duration_hours_minutes), hours, minutes);
        if (hours > 0) return String.format(java.util.Locale.ROOT, AppLanguage.localizedContext(context).getString(R.string.background_duration_hours), hours);
        return String.format(java.util.Locale.ROOT, AppLanguage.localizedContext(context).getString(R.string.background_duration_minutes), minutes);
    }

    private static String formatTime(android.content.Context context, int hour, int minute) {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        android.content.Context localized = AppLanguage.localizedContext(context);
        return new java.text.SimpleDateFormat(
                android.text.format.DateFormat.is24HourFormat(localized) ? "HH:mm" : "h:mm a",
                java.util.Locale.US).format(time.getTime());
    }

    public String getRepeatDescription(android.content.Context context) {
        switch (repeatType) {
            case ONCE:
                return AppLanguage.localizedContext(context).getString(R.string.schedule_repeat_once);
            case DAILY:
                return AppLanguage.localizedContext(context).getString(R.string.schedule_repeat_daily);
            case WEEKLY:
                if (repeatDays.isEmpty()) {
                    return AppLanguage.localizedContext(context).getString(R.string.schedule_repeat_weekly);
                }
                StringBuilder days = new StringBuilder();
                String[] dayNames = new java.text.DateFormatSymbols(AppLanguage.localizedContext(context).getResources().getConfiguration().getLocales().get(0)).getShortWeekdays();
                for (int day : repeatDays) {
                    if (days.length() > 0) days.append(", ");
                    days.append(dayNames[day]);
                }
                return days.toString();
            default:
                return AppLanguage.localizedContext(context).getString(R.string.background_unknown);
        }
    }
    
    public String getPreNotifyDescription(android.content.Context context) {
        if (!preNotifyEnabled) {
            return AppLanguage.localizedContext(context).getString(R.string.background_no_notification);
        }
        return String.format(java.util.Locale.ROOT, AppLanguage.localizedContext(context).getString(R.string.schedule_notify_minutes_before), preNotifyMinutes);
    }
} 