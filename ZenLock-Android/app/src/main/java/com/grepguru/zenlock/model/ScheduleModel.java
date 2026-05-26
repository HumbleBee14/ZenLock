package com.grepguru.zenlock.model;

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
        ONCE("Once"),
        DAILY("Daily"),
        WEEKLY("Weekly");
        
        private final String displayName;
        
        RepeatType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
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
    public String getFormattedStartTime() {
        return String.format("%02d:%02d", startHour, startMinute);
    }
    
    public String getFormattedEndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMinute);
        calendar.add(Calendar.MINUTE, focusDurationMinutes);
        
        return String.format("%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY), 
            calendar.get(Calendar.MINUTE));
    }
    
    public String getFormattedDuration() {
        int hours = focusDurationMinutes / 60;
        int minutes = focusDurationMinutes % 60;
        
        if (hours > 0) {
            return String.format("%d hr %d min", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }
    
    public String getRepeatDescription() {
        switch (repeatType) {
            case ONCE:
                return "Once";
            case DAILY:
                return "Daily";
            case WEEKLY:
                if (repeatDays.isEmpty()) {
                    return "Weekly";
                }
                StringBuilder days = new StringBuilder();
                String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                for (int day : repeatDays) {
                    if (days.length() > 0) days.append(", ");
                    days.append(dayNames[day - 1]);
                }
                return days.toString();
            default:
                return "Unknown";
        }
    }
    
    public String getPreNotifyDescription() {
        if (!preNotifyEnabled) {
            return "No notification";
        }
        return preNotifyMinutes + " min before";
    }
} 