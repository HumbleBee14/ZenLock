package com.grepguru.zenlock.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity for weekly aggregated statistics
 * Stores consolidated weekly analytics data for trend analysis
 */
@Entity(tableName = "weekly_stats")
public class WeeklyStatsEntity {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "week_key")
    public String weekKey; // YYYY-WW format (e.g., "2024-W37")
    
    @ColumnInfo(name = "total_sessions")
    public int totalSessions;
    
    @ColumnInfo(name = "total_focus_time")
    public long totalFocusTime; // in milliseconds
    
    @ColumnInfo(name = "total_mobile_usage")
    public long totalMobileUsage; // in milliseconds
    
    @ColumnInfo(name = "avg_daily_focus_time")
    public long avgDailyFocusTime; // in milliseconds
    
    @ColumnInfo(name = "completion_rate")
    public float completionRate; // percentage
    
    @ColumnInfo(name = "avg_focus_score")
    public float avgFocusScore;
    
    @ColumnInfo(name = "best_day_focus_time")
    public long bestDayFocusTime; // in milliseconds
    
    @ColumnInfo(name = "best_day_date")
    public String bestDayDate; // YYYY-MM-DD
    
    @ColumnInfo(name = "total_whitelisted_time")
    public long totalWhitelistedTime; // in milliseconds
    
    @ColumnInfo(name = "notes")
    public String notes; // User notes for the week
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    
    // Default constructor required by Room
    public WeeklyStatsEntity() {}
    
    // Constructor for creating new weekly stats
    @Ignore
    public WeeklyStatsEntity(String weekKey, int totalSessions, long totalFocusTime, 
                            long totalMobileUsage, long avgDailyFocusTime, 
                            float completionRate, float avgFocusScore, 
                            long bestDayFocusTime, String bestDayDate, 
                            long totalWhitelistedTime) {
        this.weekKey = weekKey;
        this.totalSessions = totalSessions;
        this.totalFocusTime = totalFocusTime;
        this.totalMobileUsage = totalMobileUsage;
        this.avgDailyFocusTime = avgDailyFocusTime;
        this.completionRate = completionRate;
        this.avgFocusScore = avgFocusScore;
        this.bestDayFocusTime = bestDayFocusTime;
        this.bestDayDate = bestDayDate;
        this.totalWhitelistedTime = totalWhitelistedTime;
        this.notes = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Utility methods
    public String getFormattedTotalFocusTime() {
        long minutes = totalFocusTime / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    public String getFormattedAvgDailyFocusTime() {
        long minutes = avgDailyFocusTime / (60 * 1000);
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
    
    public int getYear() {
        return Integer.parseInt(weekKey.split("-W")[0]);
    }
    
    public int getWeekNumber() {
        return Integer.parseInt(weekKey.split("-W")[1]);
    }
}
