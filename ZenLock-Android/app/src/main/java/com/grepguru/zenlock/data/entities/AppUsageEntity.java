package com.grepguru.zenlock.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for app usage during focus sessions
 * Tracks which whitelisted apps were used and for how long during each session
 */
@Entity(tableName = "app_usage",
        foreignKeys = @ForeignKey(entity = SessionEntity.class,
                                parentColumns = "session_id",
                                childColumns = "session_id",
                                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "session_id")})
public class AppUsageEntity {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;
    
    @ColumnInfo(name = "session_id")
    public long sessionId;
    
    @ColumnInfo(name = "package_name")
    public String packageName;
    
    @ColumnInfo(name = "app_name")
    public String appName;
    
    @ColumnInfo(name = "usage_time")
    public long usageTime; // in milliseconds
    
    @ColumnInfo(name = "is_whitelisted")
    public boolean isWhitelisted;
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    // Default constructor required by Room
    public AppUsageEntity() {}
    
    // Constructor for creating new app usage records
    @Ignore
    public AppUsageEntity(long sessionId, String packageName, String appName, 
                         long usageTime, boolean isWhitelisted) {
        this.sessionId = sessionId;
        this.packageName = packageName;
        this.appName = appName;
        this.usageTime = usageTime;
        this.isWhitelisted = isWhitelisted;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Utility methods
    public String getFormattedUsageTime() {
        long minutes = usageTime / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return String.format("%ds", usageTime / 1000);
        }
    }
    
    public double getUsagePercentage(long totalSessionTime) {
        if (totalSessionTime == 0) return 0;
        return (double) usageTime / totalSessionTime * 100;
    }
}
