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
                // Store data for the last 7 days to ensure we have recent data
                for (int i = 1; i <= 7; i++) {
                    String date = getDateDaysAgo(i);
                    
                    // Check if we already have data for this date
                    DailyMobileUsageEntity existing = database.analyticsDao().getDailyMobileUsage(date);
                    if (existing == null) {
                        // Get mobile usage from UsageStatsManager
                        long mobileUsageMs = mobileUsageTracker.getMobileUsageForDate(date);
                        
                        // Create and store the entity
                        DailyMobileUsageEntity entity = new DailyMobileUsageEntity(date, mobileUsageMs);
                        database.analyticsDao().insertDailyMobileUsage(entity);
                    }
                }
                
                // Maintain FIFO behavior - keep only last 30 days
                maintainMax30Days();
                
            } catch (Exception e) {
                Log.e(TAG, "Error storing mobile usage data", e);
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
            
            // Check if this is a recent date (within last 7 days) before calling slow API
            if (isRecentDate(date)) {
                // Only fetch from UsageStatsManager for very recent dates (today, yesterday, etc.)
                return mobileUsageTracker.getMobileUsageForDate(date);
            } else {
                // For older dates, return 0 instead of calling slow API
                // This prevents the context error and makes charts load faster
                return 0;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mobile usage for date: " + date, e);
            return 0;
        }
    }
    
    /**
     * Check if a date is recent (within last 7 days)
     */
    private boolean isRecentDate(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date targetDate = sdf.parse(date);
            if (targetDate == null) return false;
            
            Date now = new Date();
            long diffInMillis = now.getTime() - targetDate.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            
            return diffInDays <= 7; // Only consider dates within last 7 days as "recent"
        } catch (Exception e) {
            return false;
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
     * Get date N days ago in YYYY-MM-DD format
     */
    private String getDateDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
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
     * Pre-populate database with recent data (last 7 days)
     * This should be called when the app starts to ensure we have data
     */
    public void prePopulateRecentData() {
        executor.execute(() -> {
            try {
                // Store data for the last 7 days to ensure we have recent data
                for (int i = 1; i <= 7; i++) {
                    String date = getDateDaysAgo(i);
                    
                    // Check if we already have data for this date
                    DailyMobileUsageEntity existing = database.analyticsDao().getDailyMobileUsage(date);
                    if (existing == null) {
                        // Get mobile usage from UsageStatsManager
                        long mobileUsageMs = mobileUsageTracker.getMobileUsageForDate(date);
                        
                        // Create and store the entity
                        DailyMobileUsageEntity entity = new DailyMobileUsageEntity(date, mobileUsageMs);
                        database.analyticsDao().insertDailyMobileUsage(entity);
                    }
                }
                
                // Maintain FIFO behavior - keep only last 30 days
                maintainMax30Days();
                
            } catch (Exception e) {
                Log.e(TAG, "Error pre-populating recent data", e);
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
