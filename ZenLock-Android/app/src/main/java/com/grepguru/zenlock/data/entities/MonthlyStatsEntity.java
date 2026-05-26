package com.grepguru.zenlock.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity for monthly aggregated statistics
 * Stores consolidated monthly analytics data for long-term trend analysis
 */
@Entity(tableName = "monthly_stats")
public class MonthlyStatsEntity {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "month_key")
    public String monthKey; // YYYY-MM format (e.g., "2024-09")
    
    @ColumnInfo(name = "total_sessions")
    public int totalSessions;
    
    @ColumnInfo(name = "total_focus_time")
    public long totalFocusTime; // in milliseconds
    
    @ColumnInfo(name = "total_mobile_usage")
    public long totalMobileUsage; // in milliseconds
    
    @ColumnInfo(name = "avg_daily_focus_time")
    public long avgDailyFocusTime; // in milliseconds
    
    @ColumnInfo(name = "avg_weekly_focus_time")
    public long avgWeeklyFocusTime; // in milliseconds
    
    @ColumnInfo(name = "completion_rate")
    public float completionRate; // percentage
    
    @ColumnInfo(name = "avg_focus_score")
    public float avgFocusScore;
    
    @ColumnInfo(name = "best_week_focus_time")
    public long bestWeekFocusTime; // in milliseconds
    
    @ColumnInfo(name = "best_week_key")
    public String bestWeekKey; // YYYY-WW
    
    @ColumnInfo(name = "total_whitelisted_time")
    public long totalWhitelistedTime; // in milliseconds
    
    @ColumnInfo(name = "active_days")
    public int activeDays; // Days with at least one session
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    
    // Default constructor required by Room
    public MonthlyStatsEntity() {}
    
    // Constructor for creating new monthly stats
    @Ignore
    public MonthlyStatsEntity(String monthKey, int totalSessions, long totalFocusTime, 
                             long totalMobileUsage, long avgDailyFocusTime, 
                             long avgWeeklyFocusTime, float completionRate, 
                             float avgFocusScore, long bestWeekFocusTime, 
                             String bestWeekKey, long totalWhitelistedTime, int activeDays) {
        this.monthKey = monthKey;
        this.totalSessions = totalSessions;
        this.totalFocusTime = totalFocusTime;
        this.totalMobileUsage = totalMobileUsage;
        this.avgDailyFocusTime = avgDailyFocusTime;
        this.avgWeeklyFocusTime = avgWeeklyFocusTime;
        this.completionRate = completionRate;
        this.avgFocusScore = avgFocusScore;
        this.bestWeekFocusTime = bestWeekFocusTime;
        this.bestWeekKey = bestWeekKey;
        this.totalWhitelistedTime = totalWhitelistedTime;
        this.activeDays = activeDays;
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
    
    public double getActivityRate() {
        int daysInMonth = getDaysInMonth();
        return daysInMonth > 0 ? (double) activeDays / daysInMonth * 100 : 0;
    }
    
    public long getActualLockedTime() {
        return totalFocusTime - totalWhitelistedTime;
    }
    
    public int getYear() {
        return Integer.parseInt(monthKey.split("-")[0]);
    }
    
    public int getMonth() {
        return Integer.parseInt(monthKey.split("-")[1]);
    }
    
    private int getDaysInMonth() {
        int year = getYear();
        int month = getMonth();
        
        switch (month) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                return 31;
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            default:
                return 30;
        }
    }
}
