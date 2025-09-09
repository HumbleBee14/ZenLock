package com.grepguru.zenlock.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.grepguru.zenlock.data.dao.AnalyticsDao;
import com.grepguru.zenlock.data.entities.AppUsageEntity;
import com.grepguru.zenlock.data.entities.DailyStatsEntity;
import com.grepguru.zenlock.data.entities.MonthlyStatsEntity;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.data.entities.WeeklyStatsEntity;

/**
 * Room database for ZenLock Analytics
 * Manages all analytics data including sessions, app usage, and aggregated statistics
 */
@Database(
    entities = {
        SessionEntity.class,
        AppUsageEntity.class,
        DailyStatsEntity.class,
        WeeklyStatsEntity.class,
        MonthlyStatsEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AnalyticsDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "zenlock_analytics_database";
    
    public abstract AnalyticsDao analyticsDao();
    
    private static volatile AnalyticsDatabase INSTANCE;
    
    /**
     * Get database instance using singleton pattern
     * Thread-safe implementation for concurrent access
     */
    public static AnalyticsDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AnalyticsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AnalyticsDatabase.class,
                        DATABASE_NAME
                    )
                    // Allow queries on main thread for simple operations (not recommended for complex queries)
                    .allowMainThreadQueries()
                    // Fallback to destructive migration for now (will add proper migrations later)
                    .fallbackToDestructiveMigration()
                    // Add callback for database creation
                    .addCallback(roomDatabaseCallback)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Database callback for initialization
     */
    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Database created, can perform any initialization here
            
            // Create indexes for better performance
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_start_time ON sessions(start_time)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_date ON sessions(date(start_time/1000, 'unixepoch'))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_app_usage_session_id ON app_usage(session_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_stats_date ON daily_stats(date)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weekly_stats_week_key ON weekly_stats(week_key)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_monthly_stats_month_key ON monthly_stats(month_key)");
        }
        
        @Override
        public void onOpen(androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Database opened, can perform any maintenance here
        }
    };
    
    /**
     * Close database instance (for testing or cleanup)
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
    
    /**
     * Get database size in bytes
     */
    public long getDatabaseSize() {
        return new java.io.File(this.getOpenHelper().getWritableDatabase().getPath()).length();
    }
    
    /**
     * Get formatted database size
     */
    public String getFormattedDatabaseSize() {
        long sizeInBytes = getDatabaseSize();
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeInBytes / 1024.0);
        } else {
            return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0));
        }
    }
}
