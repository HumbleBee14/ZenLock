package com.grepguru.zenlock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.grepguru.zenlock.data.entities.AppUsageEntity;
import com.grepguru.zenlock.data.entities.DailyStatsEntity;
import com.grepguru.zenlock.data.entities.MonthlyStatsEntity;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.data.entities.WeeklyStatsEntity;
import com.grepguru.zenlock.data.repository.AnalyticsRepository;
import com.grepguru.zenlock.model.AnalyticsModels;
import com.grepguru.zenlock.BuildConfig;

import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enhanced AnalyticsManager using Room database
 * Provides a clean interface for analytics operations while using SQLite for data persistence
 */
public class AnalyticsManager {
    private static final String TAG = "AnalyticsManager";
    
    // Repository for database operations
    private AnalyticsRepository repository;
    private Context context;
    private MobileUsageTracker mobileUsageTracker;
    
    // Current session tracking (still using SharedPreferences for active session state)
    private static final String SESSION_PREFS = "CurrentSessionPrefs";
    private SharedPreferences sessionPrefs;
    
    // Current session data
    private long currentSessionStart;
    private long currentSessionTarget;
    private String currentSessionSource;
    private Map<String, Long> currentSessionAppUsage = new HashMap<>();
    
    public AnalyticsManager(Context context) {
        this.context = context;
        this.repository = new AnalyticsRepository(context);
        this.sessionPrefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE);
        this.mobileUsageTracker = new MobileUsageTracker(context);
        
        // Restore current session state if exists
        restoreCurrentSessionState();
        
        // Update today's mobile usage if permission is available (only once per app launch)
        updateTodayMobileUsageIfAvailable();
    }
    
    // =====================================
    // SESSION LIFECYCLE METHODS
    // =====================================
    
    /**
     * Start a new focus session
     */
    public void startSession(long targetDurationMillis) {
        startSession(targetDurationMillis, "manual");
    }
    
    /**
     * Start a new focus session with source
     */
    public void startSession(long targetDurationMillis, String source) {
        currentSessionStart = System.currentTimeMillis();
        currentSessionTarget = targetDurationMillis;
        currentSessionSource = source;
        currentSessionAppUsage.clear();
        
        // Save current session state
        saveCurrentSessionState();
        
        Log.d(TAG, "Session started: " + formatDuration(targetDurationMillis) + " from " + source);
    }
    
    /**
     * Record app usage during current session
     */
    public void recordAppUsage(String packageName, long usageTime) {
        if (currentSessionStart > 0) {
            String appName = getAppName(packageName);
            currentSessionAppUsage.put(packageName, 
                currentSessionAppUsage.getOrDefault(packageName, 0L) + usageTime);
            
            // Update session state
            saveCurrentSessionState();
            
            Log.d(TAG, "App usage recorded: " + appName + " (" + formatDuration(usageTime) + ")");
        }
    }
    
    /**
     * Record app access (legacy compatibility method)
     */
    public void recordAppAccess(String packageName) {
        // Default to 1 second of usage time for compatibility
        recordAppUsage(packageName, 1000);
    }
    
    /**
     * Record blocked app attempt
     */
    public void recordBlockedAttempt(String packageName) {
        // For now, just log the blocked attempt
        // This could be expanded to track blocked attempts separately
        Log.d(TAG, "App blocked: " + packageName);
    }
    
    /**
     * End current focus session
     */
    public void endSession(boolean completed) {
        if (currentSessionStart == 0) {
            Log.w(TAG, "No active session to end");
            return;
        }
        
        long endTime = System.currentTimeMillis();
        long actualDuration = endTime - currentSessionStart;
        int focusScore = calculateFocusScore(actualDuration, currentSessionTarget);
        
        // Create session entity
        SessionEntity session = new SessionEntity(
            System.currentTimeMillis(), // Use current time as session ID
            currentSessionStart,
            endTime,
            currentSessionTarget,
            actualDuration,
            completed,
            currentSessionSource,
            focusScore
        );
        
        // Create app usage entities
        List<AppUsageEntity> appUsages = new ArrayList<>();
        for (Map.Entry<String, Long> entry : currentSessionAppUsage.entrySet()) {
            AppUsageEntity appUsage = new AppUsageEntity(
                session.sessionId,
                entry.getKey(),
                getAppName(entry.getKey()),
                entry.getValue(),
                isWhitelisted(entry.getKey())
            );
            appUsages.add(appUsage);
        }
        
        // Save session to database
        repository.insertSession(session, appUsages);
        
        // Clear current session
        clearCurrentSessionState();
        
        Log.d(TAG, "Session ended: " + (completed ? "COMPLETED" : "INTERRUPTED") + 
              " Duration: " + formatDuration(actualDuration) + 
              " Target: " + formatDuration(currentSessionTarget) +
              " Score: " + focusScore);
    }
    
    // =====================================
    // DATA RETRIEVAL METHODS (NEW ROOM-BASED)
    // =====================================
    
    /**
     * Get today's statistics (new Room-based method)
     */
    public LiveData<DailyStatsEntity> getTodayStatsLive() {
        return repository.getTodayStats();
    }
    
    /**
     * Get today's statistics (legacy compatibility method)
     */
    public AnalyticsModels.DailyStats getTodayStats() {
        // Legacy compatibility method
        // Should be replaced with getTodayStatsLive() which returns LiveData
        String today = getCurrentDate();
        AnalyticsModels.DailyStats stats = new AnalyticsModels.DailyStats(today);
        
        // Note: This is a simplified implementation for backward compatibility
        // The UI should be updated to use getTodayStatsLive() for real data
        return stats;
    }
    
    /**
     * Get yesterday's statistics for comparison
     */
    public DailyStatsEntity getYesterdayStats() {
        return repository.getYesterdayStats();
    }
    
    /**
     * Get current week statistics
     */
    public LiveData<WeeklyStatsEntity> getCurrentWeekStats() {
        return repository.getCurrentWeekStats();
    }
    
    /**
     * Get week stats (legacy compatibility method)
     */
    public AnalyticsModels.PeriodSummary getWeekStats() {
        // Legacy compatibility method
        // Should be replaced with getCurrentWeekStats() which returns LiveData
        String weekKey = getCurrentWeekKey();
        AnalyticsModels.PeriodSummary summary = new AnalyticsModels.PeriodSummary(weekKey);
        
        // Note: This is a simplified implementation for backward compatibility
        return summary;
    }
    
    /**
     * Get last week statistics for comparison
     */
    public WeeklyStatsEntity getLastWeekStats() {
        return repository.getLastWeekStats();
    }
    
    /**
     * Get current month statistics
     */
    public LiveData<MonthlyStatsEntity> getCurrentMonthStats() {
        return repository.getCurrentMonthStats();
    }
    
    /**
     * Get last month statistics for comparison
     */
    public MonthlyStatsEntity getLastMonthStats() {
        return repository.getLastMonthStats();
    }
    
    /**
     * Get recent sessions for display (new Room-based method)
     */
    public LiveData<List<SessionEntity>> getRecentSessionsLive(int limit) {
        return repository.getRecentSessions(limit);
    }
    
    /**
     * Get recent sessions (legacy compatibility method)
     */
    public List<AnalyticsModels.FocusSession> getRecentSessions(int limit) {
        // Legacy compatibility method
        // Should be replaced with getRecentSessionsLive() which returns LiveData
        List<AnalyticsModels.FocusSession> sessions = new ArrayList<>();
        
        // Note: This is a simplified implementation for backward compatibility
        // The UI should be updated to use getRecentSessionsLive() for real data
        return sessions;
    }
    
    /**
     * Get app usage for a specific session
     */
    public LiveData<List<AppUsageEntity>> getAppUsageForSession(long sessionId) {
        return repository.getAppUsageForSession(sessionId);
    }
    
    /**
     * Get sessions for a specific date
     */
    public LiveData<List<SessionEntity>> getSessionsForDate(String date) {
        return repository.getSessionsForDate(date);
    }
    
    // =====================================
    // LEGACY COMPATIBILITY METHODS
    // =====================================
    
    
    // =====================================
    // UTILITY METHODS
    // =====================================
    
    /**
     * Check if there's an active session
     */
    public boolean hasActiveSession() {
        return currentSessionStart > 0;
    }
    
    /**
     * Get current session progress
     */
    public double getCurrentSessionProgress() {
        if (currentSessionStart == 0 || currentSessionTarget == 0) {
            return 0.0;
        }
        
        long elapsed = System.currentTimeMillis() - currentSessionStart;
        return Math.min(100.0, (double) elapsed / currentSessionTarget * 100);
    }
    
    // Mobile usage update method removed - data is fetched fresh from UsageStatsManager
    
    /**
     * Get mobile usage tracker instance
     */
    public MobileUsageTracker getMobileUsageTracker() {
        return mobileUsageTracker;
    }
    
    /**
     * Get this week's total focus time
     */
    public long getThisWeekFocusTime() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            long weekStart = calendar.getTimeInMillis();
            long weekEnd = weekStart + (7 * 24 * 60 * 60 * 1000);
            
            return repository.getTotalFocusTimeForPeriod(weekStart, weekEnd);
        } catch (Exception e) {
            Log.e(TAG, "Error getting this week's focus time", e);
            return 0;
        }
    }
    
    /**
     * Get last week's total focus time
     */
    public long getLastWeekFocusTime() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            long thisWeekStart = calendar.getTimeInMillis();
            long lastWeekStart = thisWeekStart - (7 * 24 * 60 * 60 * 1000);
            long lastWeekEnd = thisWeekStart;
            
            return repository.getTotalFocusTimeForPeriod(lastWeekStart, lastWeekEnd);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last week's focus time", e);
            return 0;
        }
    }
    
    /**
     * Get this week's mobile usage
     */
    public long getThisWeekMobileUsage() {
        try {
            return mobileUsageTracker.getThisWeekMobileUsage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting this week's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get last week's mobile usage
     */
    public long getLastWeekMobileUsage() {
        try {
            return mobileUsageTracker.getLastWeekMobileUsage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting last week's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get this month's total focus time
     */
    public long getThisMonthFocusTime() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            long monthStart = calendar.getTimeInMillis();
            long monthEnd = monthStart + (30 * 24 * 60 * 60 * 1000); // Approximate month
            
            return repository.getTotalFocusTimeForPeriod(monthStart, monthEnd);
        } catch (Exception e) {
            Log.e(TAG, "Error getting this month's focus time", e);
            return 0;
        }
    }
    
    /**
     * Get last month's total focus time
     */
    public long getLastMonthFocusTime() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            long thisMonthStart = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, -1);
            long lastMonthStart = calendar.getTimeInMillis();
            long lastMonthEnd = thisMonthStart;
            
            return repository.getTotalFocusTimeForPeriod(lastMonthStart, lastMonthEnd);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last month's focus time", e);
            return 0;
        }
    }
    
    /**
     * Get this month's mobile usage
     */
    public long getThisMonthMobileUsage() {
        try {
            return mobileUsageTracker.getThisMonthMobileUsage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting this month's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get last month's mobile usage
     */
    public long getLastMonthMobileUsage() {
        try {
            return mobileUsageTracker.getLastMonthMobileUsage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting last month's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Check if usage stats permission is available
     */
    public boolean hasUsageStatsPermission() {
        return UsageStatsPermissionManager.hasUsageStatsPermission(context);
    }
    
    /**
     * Request usage stats permission
     */
    public void requestUsageStatsPermission() {
        UsageStatsPermissionManager.requestUsageStatsPermission(context);
    }
    
    /**
     * Update today's mobile usage if permission is available
     * NOTE: We don't store mobile usage in database - always fetch fresh from UsageStatsManager
     */
    public void updateTodayMobileUsageIfAvailable() {
        // No need to store mobile usage in database - always fetch fresh
        // This method is kept for compatibility but does nothing
        Log.d(TAG, "Mobile usage will be fetched fresh from UsageStatsManager when needed");
    }
    
    /**
     * Get today's mobile usage in formatted string
     */
    public String getTodayMobileUsageFormatted() {
        if (!hasUsageStatsPermission()) {
            return "Permission needed";
        }
        return mobileUsageTracker.getFormattedTodayUsage();
    }
    
    /**
     * Get detailed usage breakdown for debugging discrepancies
     */
    public String getDetailedUsageBreakdown() {
        return mobileUsageTracker.getDetailedUsageInfo();
    }
    
    
    /**
     * Compare our calculation with different methods
     */
    public void debugUsageCalculation() {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Cannot debug usage - permission not granted");
            return;
        }
        
        Log.d(TAG, "=== USAGE CALCULATION DEBUG ===");
        
        // Method 1: Our current calculation (INTERVAL_DAILY)
        long ourCalculation = mobileUsageTracker.getTodayMobileUsage();
        Log.d(TAG, "Method 1 (INTERVAL_DAILY): " + formatDuration(ourCalculation));
        
        // Method 2: Try different interval
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            long startTime = cal.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> weeklyStats = ((UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE))
                .queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime);
            
            long alternativeTotal = 0;
            for (UsageStats stats : weeklyStats) {
                if (!stats.getPackageName().equals(context.getPackageName())) {
                    alternativeTotal += stats.getTotalTimeInForeground();
                }
            }
            
            Log.d(TAG, "Method 2 (INTERVAL_BEST, 24h): " + formatDuration(alternativeTotal));
            
        } catch (Exception e) {
            Log.e(TAG, "Error with alternative calculation", e);
        }
        
        Log.d(TAG, "===============================");
    }
    
    /**
     * Update weekly notes
     */
    public void updateWeeklyNotes(String notes) {
        repository.updateWeeklyNotes(getCurrentWeekKey(), notes);
    }
    
    /**
     * Cleanup old data
     */
    public void cleanupOldData() {
        repository.cleanupOldData();
    }
    
    // =====================================
    // PRIVATE HELPER METHODS
    // =====================================
    
    private void saveCurrentSessionState() {
        SharedPreferences.Editor editor = sessionPrefs.edit();
        editor.putLong("session_start", currentSessionStart);
        editor.putLong("session_target", currentSessionTarget);
        editor.putString("session_source", currentSessionSource);
        
        // Save app usage as JSON or simple format
        StringBuilder appUsageStr = new StringBuilder();
        for (Map.Entry<String, Long> entry : currentSessionAppUsage.entrySet()) {
            if (appUsageStr.length() > 0) appUsageStr.append(";");
            appUsageStr.append(entry.getKey()).append(":").append(entry.getValue());
        }
        editor.putString("app_usage", appUsageStr.toString());
        editor.apply();
    }
    
    private void restoreCurrentSessionState() {
        currentSessionStart = sessionPrefs.getLong("session_start", 0);
        currentSessionTarget = sessionPrefs.getLong("session_target", 0);
        currentSessionSource = sessionPrefs.getString("session_source", "manual");
        
        // Restore app usage
        String appUsageStr = sessionPrefs.getString("app_usage", "");
        currentSessionAppUsage.clear();
        if (!appUsageStr.isEmpty()) {
            String[] entries = appUsageStr.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    currentSessionAppUsage.put(parts[0], Long.parseLong(parts[1]));
                }
            }
        }
    }
    
    private void clearCurrentSessionState() {
        currentSessionStart = 0;
        currentSessionTarget = 0;
        currentSessionSource = "";
        currentSessionAppUsage.clear();
        
        // Clear from SharedPreferences
        sessionPrefs.edit().clear().apply();
    }
    
    private int calculateFocusScore(long actualDuration, long targetDuration) {
        if (targetDuration == 0) return 0;
        return (int) Math.min(100, (double) actualDuration / targetDuration * 100);
    }
    
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
    
    private String getCurrentWeekKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-'W'ww", Locale.US);
        return sdf.format(new Date());
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format(Locale.US, "%dh %dm", hours, minutes % 60);
        } else {
            return String.format(Locale.US, "%dm", minutes);
        }
    }
    
    private String getAppName(String packageName) {
        // Get app name from package name
        // This should use AppUtils or similar utility
        return packageName; // Simplified for now
    }
    
    private boolean isWhitelisted(String packageName) {
        // Check if app is whitelisted
        // This should use WhitelistManager
        return false; // Simplified for now
    }
    
    /**
     * Create sample analytics data for testing (development only)
     * WARNING: This method should only be called manually for testing
     */
    public void createSampleData() {
        if (!BuildConfig.DEBUG) return; // Only in debug builds
        
        Log.w(TAG, "⚠️ WARNING: Creating sample analytics data - this should only be used for testing!");
        Log.d(TAG, "Creating sample analytics data...");
        
        // Create a few sample sessions
        long now = System.currentTimeMillis();
        
        // Today's session 1 - Completed
        SessionEntity session1 = new SessionEntity(
            now - 1000,
            now - (3600 * 1000), // 1 hour ago
            now - (1800 * 1000), // 30 minutes ago
            3600 * 1000, // 1 hour target
            1800 * 1000, // 30 minutes actual
            false, // Not completed (interrupted)
            "manual",
            50 // 50% focus score
        );
        
        // Today's session 2 - Completed
        SessionEntity session2 = new SessionEntity(
            now - 2000,
            now - (7200 * 1000), // 2 hours ago
            now - (3600 * 1000), // 1 hour ago
            3600 * 1000, // 1 hour target
            3600 * 1000, // 1 hour actual
            true, // Completed
            "schedule:Morning Focus",
            100 // 100% focus score
        );
        
        // Save sample sessions
        List<AppUsageEntity> sampleAppUsage = new ArrayList<>();
        sampleAppUsage.add(new AppUsageEntity(session1.sessionId, "com.android.phone", "Phone", 300000, true));
        
        repository.insertSession(session1, sampleAppUsage);
        repository.insertSession(session2, new ArrayList<>());
        
        Log.d(TAG, "Sample data created successfully");
    }
    
    /**
     * Debug method to check for duplicate sessions
     */
    public void checkForDuplicates() {
        // This would check the database for duplicate sessions
        // For now, just log the current session count
        Log.d(TAG, "=== SESSION DUPLICATE CHECK ===");
        Log.d(TAG, "Current session active: " + hasActiveSession());
        Log.d(TAG, "Session start time: " + currentSessionStart);
        Log.d(TAG, "Session target: " + formatDuration(currentSessionTarget));
        Log.d(TAG, "Session source: " + currentSessionSource);
        Log.d(TAG, "===============================");
    }
    
    /**
     * Close analytics manager and cleanup resources
     */
    public void close() {
        if (repository != null) {
            repository.close();
        }
    }
}