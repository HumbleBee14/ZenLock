package com.grepguru.zenlock.data.entities;

import com.grepguru.zenlock.utils.DurationText;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity for daily mobile usage data
 * Stores processed mobile usage data with FIFO-like behavior (max 30 days)
 * This solves Android's UsageStatsManager data retention limitations
 */
@Entity(tableName = "daily_mobile_usage")
public class DailyMobileUsageEntity {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "date")
    public String date; // YYYY-MM-DD format
    
    @ColumnInfo(name = "total_mobile_usage")
    public long totalMobileUsage; // in milliseconds
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    
    // Default constructor required by Room
    public DailyMobileUsageEntity() {}
    
    // Constructor for creating new daily mobile usage
    @Ignore
    public DailyMobileUsageEntity(String date, long totalMobileUsage) {
        this.date = date;
        this.totalMobileUsage = totalMobileUsage;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Utility methods
    public String getFormattedUsage(android.content.Context context) {
        long minutes = totalMobileUsage / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return DurationText.minutes(context, hours * 60 + minutes);
        } else {
            return DurationText.minutes(context, minutes);
        }
    }
    
    public long getUsageInMinutes() {
        return totalMobileUsage / (60 * 1000);
    }
    
    public long getUsageInHours() {
        return totalMobileUsage / (60 * 60 * 1000);
    }
    
    public boolean isEmpty() {
        return totalMobileUsage <= 0;
    }
}
