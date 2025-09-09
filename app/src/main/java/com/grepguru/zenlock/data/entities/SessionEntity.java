package com.grepguru.zenlock.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity for focus sessions
 * Stores individual session data including timing, completion status, and metadata
 */
@Entity(tableName = "sessions")
public class SessionEntity {
    
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    public long sessionId;
    
    @ColumnInfo(name = "start_time")
    public long startTime;
    
    @ColumnInfo(name = "end_time")
    public long endTime;
    
    @ColumnInfo(name = "target_duration")
    public long targetDuration;
    
    @ColumnInfo(name = "actual_duration")
    public long actualDuration;
    
    @ColumnInfo(name = "completed")
    public boolean completed;
    
    @ColumnInfo(name = "source")
    public String source; // "manual", "schedule:Morning Focus"
    
    @ColumnInfo(name = "focus_score")
    public int focusScore;
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    // Default constructor required by Room
    public SessionEntity() {}
    
    // Constructor for creating new sessions
    @Ignore
    public SessionEntity(long sessionId, long startTime, long endTime, 
                        long targetDuration, long actualDuration, 
                        boolean completed, String source, int focusScore) {
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.targetDuration = targetDuration;
        this.actualDuration = actualDuration;
        this.completed = completed;
        this.source = source;
        this.focusScore = focusScore;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Utility methods
    public double getCompletionRate() {
        if (targetDuration == 0) return 0;
        return Math.min(100.0, (double) actualDuration / targetDuration * 100);
    }
    
    public String getFormattedDuration() {
        long minutes = actualDuration / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    public boolean isInterrupted() {
        return !completed && actualDuration < targetDuration * 0.8; // Less than 80% completed
    }
    
    public boolean isPartial() {
        return !completed && actualDuration >= targetDuration * 0.8; // 80% or more completed
    }
}
