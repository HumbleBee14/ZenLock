package com.grepguru.zenlock.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.grepguru.zenlock.data.dao.AnalyticsDao;
import com.grepguru.zenlock.data.database.AnalyticsDatabase;
import com.grepguru.zenlock.data.entities.AppUsageEntity;
import com.grepguru.zenlock.data.entities.DailyStatsEntity;
import com.grepguru.zenlock.data.entities.MonthlyStatsEntity;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.data.entities.WeeklyStatsEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Analytics data operations
 * Provides a clean API for accessing analytics data and handles background operations
 */
public class AnalyticsRepository {
    
    private static final String TAG = "AnalyticsRepository";
    
    private AnalyticsDao analyticsDao;
    private ExecutorService executor;
    
    public AnalyticsRepository(Context context) {
        AnalyticsDatabase db = AnalyticsDatabase.getDatabase(context);
        analyticsDao = db.analyticsDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // =====================================
    // SESSION OPERATIONS
    // =====================================
    
    /**
     * Insert a new session with app usage data
     */
    public void insertSession(SessionEntity session, List<AppUsageEntity> appUsages) {
        executor.execute(() -> {
            try {
                analyticsDao.insertSessionWithAppUsage(session, appUsages);
                Log.d(TAG, "Session inserted: " + session.sessionId);
                
                // Update daily stats after inserting session
                updateDailyStatsForDate(getDateFromTimestamp(session.startTime));
                
            } catch (Exception e) {
                Log.e(TAG, "Error inserting session", e);
            }
        });
    }
    
    /**
     * Get recent sessions (LiveData for UI)
     */
    public LiveData<List<SessionEntity>> getRecentSessions(int limit) {
        return analyticsDao.getRecentSessions(limit);
    }
    
    /**
     * Get sessions for a specific date
     */
    public LiveData<List<SessionEntity>> getSessionsForDate(String date) {
        return analyticsDao.getSessionsForDate(date);
    }
    
    /**
     * Get session by ID with app usage
     */
    public LiveData<SessionEntity> getSessionById(long sessionId) {
        return analyticsDao.getSessionById(sessionId);
    }
    
    /**
     * Get app usage for a session
     */
    public LiveData<List<AppUsageEntity>> getAppUsageForSession(long sessionId) {
        return analyticsDao.getAppUsageForSession(sessionId);
    }
    
    // =====================================
    // DAILY STATS OPERATIONS
    // =====================================
    
    /**
     * Get daily stats for a specific date
     */
    public LiveData<DailyStatsEntity> getDailyStats(String date) {
        return analyticsDao.getDailyStats(date);
    }
    
    /**
     * Get today's stats
     */
    public LiveData<DailyStatsEntity> getTodayStats() {
        return getDailyStats(getCurrentDate());
    }
    
    /**
     * Get yesterday's stats for comparison
     */
    public DailyStatsEntity getYesterdayStats() {
        return analyticsDao.getYesterdayStats();
    }
    
    /**
     * Update daily stats for a specific date
     */
    public void updateDailyStatsForDate(String date) {
        executor.execute(() -> {
            try {
                // Calculate aggregated data for the date
                int totalSessions = analyticsDao.getSessionCountForDate(date);
                Long totalFocusTime = analyticsDao.getTotalFocusTimeForDate(date);
                int completedSessions = analyticsDao.getCompletedSessionsForDate(date);
                int interruptedSessions = analyticsDao.getInterruptedSessionsForDate(date);
                Float avgFocusScore = analyticsDao.getAverageFocusScoreForDate(date);
                Long totalWhitelistedTime = analyticsDao.getTotalWhitelistedTimeForDate(date);
                
                // Handle null values
                totalFocusTime = totalFocusTime != null ? totalFocusTime : 0L;
                avgFocusScore = avgFocusScore != null ? avgFocusScore : 0f;
                totalWhitelistedTime = totalWhitelistedTime != null ? totalWhitelistedTime : 0L;
                
                // Create or update daily stats
                DailyStatsEntity dailyStats = new DailyStatsEntity(
                    date,
                    totalSessions,
                    totalFocusTime,
                    completedSessions,
                    interruptedSessions,
                    avgFocusScore,
                    totalWhitelistedTime
                );
                
                analyticsDao.insertDailyStats(dailyStats);
                Log.d(TAG, "Daily stats updated for " + date);
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating daily stats for " + date, e);
            }
        });
    }
    
    // Mobile usage update method removed - data is fetched fresh from UsageStatsManager
    
    /**
     * Get total focus time for a specific period
     */
    public long getTotalFocusTimeForPeriod(long startTime, long endTime) {
        try {
            Long result = analyticsDao.getTotalFocusTimeForPeriod(startTime, endTime);
            return result != null ? result : 0L;
        } catch (Exception e) {
            Log.e(TAG, "Error getting total focus time for period", e);
            return 0L;
        }
    }

    /**
     * Get daily stats between two dates inclusive, synchronously (UI should call from background thread)
     */
    public List<DailyStatsEntity> getDailyStatsForDateRangeSync(String startDate, String endDate) {
        try {
            return analyticsDao.getDailyStatsForDateRangeSync(startDate, endDate);
        } catch (Exception e) {
            Log.e(TAG, "Error getting daily stats for date range", e);
            return java.util.Collections.emptyList();
        }
    }
    
    // =====================================
    // WEEKLY STATS OPERATIONS
    // =====================================
    
    /**
     * Get weekly stats for a specific week
     */
    public LiveData<WeeklyStatsEntity> getWeeklyStats(String weekKey) {
        return analyticsDao.getWeeklyStats(weekKey);
    }
    
    /**
     * Get current week stats
     */
    public LiveData<WeeklyStatsEntity> getCurrentWeekStats() {
        return getWeeklyStats(getCurrentWeekKey());
    }
    
    /**
     * Get last week stats for comparison
     */
    public WeeklyStatsEntity getLastWeekStats() {
        return analyticsDao.getLastWeekStats();
    }
    
    /**
     * Update weekly stats for current week
     */
    public void updateCurrentWeekStats() {
        updateWeeklyStatsForWeek(getCurrentWeekKey());
    }
    
    /**
     * Update weekly stats for a specific week
     */
    public void updateWeeklyStatsForWeek(String weekKey) {
        executor.execute(() -> {
            try {
                // Calculate aggregated data for the week
                int totalSessions = analyticsDao.getSessionCountForWeek(weekKey);
                Long totalFocusTime = analyticsDao.getTotalFocusTimeForWeek(weekKey);
                Float avgFocusScore = analyticsDao.getAverageFocusScoreForWeek(weekKey);
                Float completionRate = analyticsDao.getCompletionRateForWeek(weekKey);
                
                // Handle null values
                totalFocusTime = totalFocusTime != null ? totalFocusTime : 0L;
                avgFocusScore = avgFocusScore != null ? avgFocusScore : 0f;
                completionRate = completionRate != null ? completionRate : 0f;
                
                long avgDailyFocusTime = totalFocusTime / 7; // Average over 7 days
                
                // Get daily stats for the week to find best day
                String[] weekDates = getWeekDates(weekKey);
                long bestDayFocusTime = 0;
                String bestDayDate = weekDates[0];
                
                for (String date : weekDates) {
                    DailyStatsEntity dayStats = analyticsDao.getDailyStatsSync(date);
                    if (dayStats != null && dayStats.totalFocusTime > bestDayFocusTime) {
                        bestDayFocusTime = dayStats.totalFocusTime;
                        bestDayDate = date;
                    }
                }
                
                // Create or update weekly stats
                WeeklyStatsEntity weeklyStats = new WeeklyStatsEntity(
                    weekKey,
                    totalSessions,
                    totalFocusTime,
                    0L, // Mobile usage will be calculated separately
                    avgDailyFocusTime,
                    completionRate,
                    avgFocusScore,
                    bestDayFocusTime,
                    bestDayDate,
                    0L // Whitelisted time will be calculated separately
                );
                
                analyticsDao.insertWeeklyStats(weeklyStats);
                Log.d(TAG, "Weekly stats updated for " + weekKey);
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating weekly stats for " + weekKey, e);
            }
        });
    }
    
    /**
     * Update notes for a weekly stats
     */
    public void updateWeeklyNotes(String weekKey, String notes) {
        executor.execute(() -> {
            try {
                WeeklyStatsEntity weeklyStats = analyticsDao.getWeeklyStatsSync(weekKey);
                if (weeklyStats != null) {
                    weeklyStats.notes = notes;
                    weeklyStats.updatedAt = System.currentTimeMillis();
                    analyticsDao.updateWeeklyStats(weeklyStats);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating weekly notes", e);
            }
        });
    }
    
    // =====================================
    // MONTHLY STATS OPERATIONS
    // =====================================
    
    /**
     * Get monthly stats for a specific month
     */
    public LiveData<MonthlyStatsEntity> getMonthlyStats(String monthKey) {
        return analyticsDao.getMonthlyStats(monthKey);
    }
    
    /**
     * Get current month stats
     */
    public LiveData<MonthlyStatsEntity> getCurrentMonthStats() {
        return getMonthlyStats(getCurrentMonthKey());
    }
    
    /**
     * Get last month stats for comparison
     */
    public MonthlyStatsEntity getLastMonthStats() {
        return analyticsDao.getLastMonthStats();
    }
    
    // =====================================
    // DATA CLEANUP OPERATIONS
    // =====================================
    
    /**
     * Cleanup old data based on retention policy
     */
    public void cleanupOldData() {
        executor.execute(() -> {
            try {
                // Calculate cutoff dates
                long sessionCutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
                String dailyCutoffDate = getDateFromTimestamp(System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)); // 1 year
                String weeklyCutoffKey = getWeekKeyFromTimestamp(System.currentTimeMillis() - (2L * 365 * 24 * 60 * 60 * 1000)); // 2 years
                String monthlyCutoffKey = getMonthKeyFromTimestamp(System.currentTimeMillis() - (5L * 365 * 24 * 60 * 60 * 1000)); // 5 years
                
                // Perform cleanup
                analyticsDao.cleanupOldData(sessionCutoffTime, dailyCutoffDate, weeklyCutoffKey, monthlyCutoffKey);
                
                Log.d(TAG, "Data cleanup completed");
                
            } catch (Exception e) {
                Log.e(TAG, "Error during data cleanup", e);
            }
        });
    }
    
    // =====================================
    // UTILITY METHODS
    // =====================================
    
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
    
    private String getCurrentWeekKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-'W'ww", Locale.US);
        return sdf.format(new Date());
    }
    
    private String getCurrentMonthKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);
        return sdf.format(new Date());
    }
    
    private String getDateFromTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    
    private String getWeekKeyFromTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-'W'ww", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    
    private String getMonthKeyFromTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    
    private String[] getWeekDates(String weekKey) {
        // Parse week key (e.g., "2024-W37")
        String[] parts = weekKey.split("-W");
        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        
        String[] dates = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        for (int i = 0; i < 7; i++) {
            dates[i] = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        return dates;
    }
    
    /**
     * Get database size information
     */
    public String getDatabaseSize() {
        try {
            AnalyticsDatabase db = AnalyticsDatabase.getDatabase(null);
            return db.getFormattedDatabaseSize();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Close repository and cleanup resources
     */
    public void close() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
