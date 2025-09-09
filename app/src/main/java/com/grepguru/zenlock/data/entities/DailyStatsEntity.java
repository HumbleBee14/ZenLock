package com.grepguru.zenlock.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity for daily aggregated statistics
 * Stores consolidated daily analytics data for quick access
 */
@Entity(tableName = "daily_stats")
public class DailyStatsEntity {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "date")
    public String date; // YYYY-MM-DD format
    
    @ColumnInfo(name = "total_sessions")
    public int totalSessions;
    
    @ColumnInfo(name = "total_focus_time")
    public long totalFocusTime; // in milliseconds
    
    @ColumnInfo(name = "total_mobile_usage")
    public long totalMobileUsage; // in milliseconds (estimated)
    
    @ColumnInfo(name = "completed_sessions")
    public int completedSessions;
    
    @ColumnInfo(name = "interrupted_sessions")
    public int interruptedSessions;
    
    @ColumnInfo(name = "avg_focus_score")
    public float avgFocusScore;
    
    @ColumnInfo(name = "total_whitelisted_time")
    public long totalWhitelistedTime; // in milliseconds
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    
    // Default constructor required by Room
    public DailyStatsEntity() {}
    
    // Constructor for creating new daily stats
    @Ignore
    public DailyStatsEntity(String date, int totalSessions, long totalFocusTime, 
                           long totalMobileUsage, int completedSessions, 
                           int interruptedSessions, float avgFocusScore, 
                           long totalWhitelistedTime) {
        this.date = date;
        this.totalSessions = totalSessions;
        this.totalFocusTime = totalFocusTime;
        this.totalMobileUsage = totalMobileUsage;
        this.completedSessions = completedSessions;
        this.interruptedSessions = interruptedSessions;
        this.avgFocusScore = avgFocusScore;
        this.totalWhitelistedTime = totalWhitelistedTime;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Utility methods
    public double getCompletionRate() {
        return totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0;
    }
    
    public long getAverageSessionDuration() {
        return totalSessions > 0 ? totalFocusTime / totalSessions : 0;
    }
    
    public String getFormattedFocusTime() {
        long minutes = totalFocusTime / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    public String getFormattedMobileUsage() {
        long minutes = totalMobileUsage / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    public double getTimeSavedPercentage() {
        return totalMobileUsage > 0 ? (double) totalFocusTime / totalMobileUsage * 100 : 0;
    }
    
    public long getActualLockedTime() {
        return totalFocusTime - totalWhitelistedTime;
    }
}
