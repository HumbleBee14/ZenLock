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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        
        // Update today's mobile usage if permission is available
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
    
    /**
     * Update mobile usage time for today
     */
    public void updateTodayMobileUsage(long mobileUsageTime) {
        repository.updateMobileUsageForDate(getCurrentDate(), mobileUsageTime);
    }
    
    /**
     * Get mobile usage tracker instance
     */
    public MobileUsageTracker getMobileUsageTracker() {
        return mobileUsageTracker;
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
     */
    public void updateTodayMobileUsageIfAvailable() {
        if (hasUsageStatsPermission()) {
            // Run in background to avoid blocking main thread
            new Thread(() -> {
                try {
                    long mobileUsage = mobileUsageTracker.updateTodayMobileUsage();
                    if (mobileUsage > 0) {
                        updateTodayMobileUsage(mobileUsage);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating mobile usage", e);
                }
            }).start();
        }
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
     */
    public void createSampleData() {
        if (!BuildConfig.DEBUG) return; // Only in debug builds
        
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
     * Close analytics manager and cleanup resources
     */
    public void close() {
        if (repository != null) {
            repository.close();
        }
    }
}