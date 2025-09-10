package com.grepguru.zenlock.utils;

import android.content.Context;
import android.util.Log;

import com.grepguru.zenlock.data.database.AnalyticsDatabase;
import com.grepguru.zenlock.data.entities.DailyMobileUsageEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager for daily mobile usage data storage and retrieval
 * Handles FIFO-like behavior with max 30 days of data
 * Solves Android's UsageStatsManager data retention limitations
 */
public class DailyMobileUsageManager {
    
    private static final String TAG = "DailyMobileUsageManager";
    private static final int MAX_DAYS = 30;
    
    private final Context context;
    private final AnalyticsDatabase database;
    private final ExecutorService executor;
    private final MobileUsageTracker mobileUsageTracker;
    
    public DailyMobileUsageManager(Context context) {
        this.context = context;
        this.database = AnalyticsDatabase.getDatabase(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mobileUsageTracker = new MobileUsageTracker(context);
    }
    
    /**
     * Store yesterday's mobile usage data
     * This should be called daily to maintain the 30-day rolling window
     */
    public void storeYesterdayMobileUsage() {
        executor.execute(() -> {
            try {
                String yesterdayDate = getYesterdayDate();
                
                // Get yesterday's mobile usage from UsageStatsManager
                long mobileUsageMs = mobileUsageTracker.getYesterdayMobileUsage();
                
                // Create and store the entity
                DailyMobileUsageEntity entity = new DailyMobileUsageEntity(yesterdayDate, mobileUsageMs);
                database.analyticsDao().insertDailyMobileUsage(entity);
                
                // Maintain FIFO behavior - keep only last 30 days
                maintainMax30Days();
                
            } catch (Exception e) {
                Log.e(TAG, "Error storing yesterday's mobile usage", e);
            }
        });
    }
    
    /**
     * Get mobile usage for a specific date
     * Returns stored data if available, otherwise fetches from UsageStatsManager
     */
    public long getMobileUsageForDate(String date) {
        try {
            // First try to get from database
            DailyMobileUsageEntity stored = database.analyticsDao().getDailyMobileUsage(date);
            if (stored != null) {
                return stored.totalMobileUsage;
            }
            
            // If not in database, fetch from UsageStatsManager (for recent dates)
            return mobileUsageTracker.getMobileUsageForDate(date);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mobile usage for date: " + date, e);
            return 0;
        }
    }
    
    /**
     * Get mobile usage for a date range
     * Uses stored data when available, falls back to UsageStatsManager for recent dates
     */
    public List<DailyMobileUsageEntity> getMobileUsageRange(String startDate, String endDate) {
        try {
            return database.analyticsDao().getDailyMobileUsageRange(startDate, endDate);
        } catch (Exception e) {
            Log.e(TAG, "Error getting mobile usage range", e);
            return null;
        }
    }
    
    /**
     * Get all stored mobile usage data
     */
    public List<DailyMobileUsageEntity> getAllStoredMobileUsage() {
        try {
            return database.analyticsDao().getAllDailyMobileUsage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all stored mobile usage", e);
            return null;
        }
    }
    
    /**
     * Maintain FIFO behavior - keep only last 30 days
     */
    private void maintainMax30Days() {
        try {
            int count = database.analyticsDao().getDailyMobileUsageCount();
            
            if (count > MAX_DAYS) {
                database.analyticsDao().maintainMax30DaysMobileUsage();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error maintaining max 30 days", e);
        }
    }
    
    /**
     * Get yesterday's date in YYYY-MM-DD format
     */
    private String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }
    
    /**
     * Get today's date in YYYY-MM-DD format
     */
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
    
    /**
     * Get date N days ago in YYYY-MM-DD format
     */
    private String getDateNDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }
    
    /**
     * Check if we have stored data for a specific date
     */
    public boolean hasStoredDataForDate(String date) {
        try {
            DailyMobileUsageEntity stored = database.analyticsDao().getDailyMobileUsage(date);
            return stored != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking stored data for date: " + date, e);
            return false;
        }
    }
    
    /**
     * Get database statistics
     */
    public void logDatabaseStats() {
        executor.execute(() -> {
            try {
                int count = database.analyticsDao().getDailyMobileUsageCount();
                String oldest = database.analyticsDao().getOldestDailyMobileUsageDate();
                String newest = database.analyticsDao().getNewestDailyMobileUsageDate();
                
                Log.d(TAG, "Mobile Usage Database Stats:");
                Log.d(TAG, "  Records: " + count);
                Log.d(TAG, "  Oldest: " + oldest);
                Log.d(TAG, "  Newest: " + newest);
                Log.d(TAG, "  Max allowed: " + MAX_DAYS);
            } catch (Exception e) {
                Log.e(TAG, "Error getting database stats", e);
            }
        });
    }
    
    /**
     * Force cleanup of old data
     */
    public void forceCleanup() {
        executor.execute(() -> {
            try {
                maintainMax30Days();
                logDatabaseStats();
            } catch (Exception e) {
                Log.e(TAG, "Error during force cleanup", e);
            }
        });
    }
    
    /**
     * Shutdown the executor
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
