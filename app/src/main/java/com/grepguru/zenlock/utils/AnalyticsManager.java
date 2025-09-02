package com.grepguru.zenlock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.grepguru.zenlock.model.AnalyticsModels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AnalyticsManager {
    private static final String TAG = "AnalyticsManager";
    private static final String PREFS_NAME = "AnalyticsPrefs";
    
    // Session tracking keys
    private static final String KEY_CURRENT_SESSION_START = "current_session_start";
    private static final String KEY_CURRENT_SESSION_TARGET = "current_session_target";
    private static final String KEY_CURRENT_SESSION_APPS = "current_session_apps";
    private static final String KEY_CURRENT_SESSION_BLOCKED = "current_session_blocked";
    
    // Daily stats keys
    private static final String KEY_DAILY_SESSIONS = "daily_sessions_";
    private static final String KEY_DAILY_TIME = "daily_time_";
    private static final String KEY_DAILY_COMPLETED = "daily_completed_";
    private static final String KEY_DAILY_BLOCKED = "daily_blocked_";
    
    // Weekly stats keys
    private static final String KEY_WEEK_SESSIONS = "week_sessions_";
    private static final String KEY_WEEK_TIME = "week_time_";
    private static final String KEY_WEEK_STREAK = "week_streak_";
    
    // Session history keys
    private static final String KEY_SESSION_HISTORY = "session_history_";
    private static final String KEY_LAST_SESSION_DATE = "last_session_date";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    
    private Context context;
    private SharedPreferences prefs;
    
    public AnalyticsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // Session Lifecycle Methods
    public void startSession(long targetDurationMillis) {
        long startTime = System.currentTimeMillis();
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_CURRENT_SESSION_START, startTime);
        editor.putLong(KEY_CURRENT_SESSION_TARGET, targetDurationMillis);
        editor.putStringSet(KEY_CURRENT_SESSION_APPS, new HashSet<>());
        editor.putStringSet(KEY_CURRENT_SESSION_BLOCKED, new HashSet<>());
        editor.apply();
        
        Log.d(TAG, "Session started: " + formatDuration(targetDurationMillis));
    }
    
    public void recordAppAccess(String packageName) {
        Set<String> apps = prefs.getStringSet(KEY_CURRENT_SESSION_APPS, new HashSet<>());
        Set<String> newApps = new HashSet<>(apps);
        newApps.add(packageName);
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_CURRENT_SESSION_APPS, newApps);
        editor.apply();
        
        Log.d(TAG, "App accessed: " + packageName);
    }
    
    public void recordBlockedAttempt(String packageName) {
        Set<String> blocked = prefs.getStringSet(KEY_CURRENT_SESSION_BLOCKED, new HashSet<>());
        Set<String> newBlocked = new HashSet<>(blocked);
        newBlocked.add(packageName);
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_CURRENT_SESSION_BLOCKED, newBlocked);
        editor.apply();
        
        Log.d(TAG, "App blocked: " + packageName);
    }
    
    public void endSession(boolean completed) {
        long startTime = prefs.getLong(KEY_CURRENT_SESSION_START, 0);
        long targetDuration = prefs.getLong(KEY_CURRENT_SESSION_TARGET, 0);
        
        if (startTime == 0) {
            Log.w(TAG, "No active session to end");
            return;
        }
        
        long endTime = System.currentTimeMillis();
        long actualDuration = endTime - startTime;
        
        // Create session object
        AnalyticsModels.FocusSession session = new AnalyticsModels.FocusSession(startTime, targetDuration);
        session.setEndTime(endTime);
        session.setCompleted(completed);
        session.setActualDuration(actualDuration);
        
        // Get apps data
        Set<String> apps = prefs.getStringSet(KEY_CURRENT_SESSION_APPS, new HashSet<>());
        Set<String> blocked = prefs.getStringSet(KEY_CURRENT_SESSION_BLOCKED, new HashSet<>());
        session.setWhitelistedApps(new ArrayList<>(apps));
        session.setBlockedAttempts(new ArrayList<>(blocked));
        
        // Save session data
        saveSessionData(session);
        
        // Clear current session
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_CURRENT_SESSION_START);
        editor.remove(KEY_CURRENT_SESSION_TARGET);
        editor.remove(KEY_CURRENT_SESSION_APPS);
        editor.remove(KEY_CURRENT_SESSION_BLOCKED);
        editor.apply();
        
        Log.d(TAG, "Session ended: " + (completed ? "COMPLETED" : "INTERRUPTED") + 
              " Duration: " + formatDuration(actualDuration) + 
              " Target: " + formatDuration(targetDuration));
    }
    
    // Data Retrieval Methods
    public AnalyticsModels.DailyStats getTodayStats() {
        String today = getCurrentDate();
        AnalyticsModels.DailyStats stats = new AnalyticsModels.DailyStats(today);
        
        stats.setTotalSessions(prefs.getInt(KEY_DAILY_SESSIONS + today, 0));
        stats.setTotalFocusTime(prefs.getLong(KEY_DAILY_TIME + today, 0));
        stats.setCompletedSessions(prefs.getInt(KEY_DAILY_COMPLETED + today, 0));
        stats.setBlockedAttempts(prefs.getInt(KEY_DAILY_BLOCKED + today, 0));
        stats.updateAverageDuration();
        
        return stats;
    }
    
    public AnalyticsModels.PeriodSummary getWeekStats() {
        String weekKey = getCurrentWeek();
        AnalyticsModels.PeriodSummary summary = new AnalyticsModels.PeriodSummary(weekKey);
        
        summary.setTotalSessions(prefs.getInt(KEY_WEEK_SESSIONS + weekKey, 0));
        summary.setTotalFocusTime(prefs.getLong(KEY_WEEK_TIME + weekKey, 0));
        summary.setLongestStreak(getCurrentStreak());
        
        return summary;
    }
    
    public List<AnalyticsModels.FocusSession> getRecentSessions(int limit) {
        List<AnalyticsModels.FocusSession> sessions = new ArrayList<>();
        
        // Get session history from SharedPreferences
        String historyKey = KEY_SESSION_HISTORY + getCurrentDate();
        Set<String> sessionHistory = prefs.getStringSet(historyKey, new HashSet<>());
        
        // Convert session history strings back to FocusSession objects
        for (String sessionData : sessionHistory) {
            if (sessions.size() >= limit) break;
            
            try {
                String[] parts = sessionData.split("\\|");
                if (parts.length >= 4) {
                    long startTime = Long.parseLong(parts[0]);
                    long duration = Long.parseLong(parts[1]);
                    boolean completed = Boolean.parseBoolean(parts[2]);
                    long targetDuration = Long.parseLong(parts[3]);
                    
                    AnalyticsModels.FocusSession session = new AnalyticsModels.FocusSession(startTime, targetDuration);
                    session.setActualDuration(duration);
                    session.setCompleted(completed);
                    session.setEndTime(startTime + duration);
                    
                    sessions.add(session);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing session data: " + sessionData);
            }
        }
        
        // Sort by start time (most recent first)
        sessions.sort((s1, s2) -> Long.compare(s2.getStartTime(), s1.getStartTime()));
        
        return sessions;
    }
    
    // Private Helper Methods
    private void saveSessionData(AnalyticsModels.FocusSession session) {
        String today = getCurrentDate();
        String weekKey = getCurrentWeek();
        
        // Update daily stats
        int dailySessions = prefs.getInt(KEY_DAILY_SESSIONS + today, 0) + 1;
        long dailyTime = prefs.getLong(KEY_DAILY_TIME + today, 0) + session.getActualDuration();
        int dailyCompleted = prefs.getInt(KEY_DAILY_COMPLETED + today, 0) + (session.isCompleted() ? 1 : 0);
        int dailyBlocked = prefs.getInt(KEY_DAILY_BLOCKED + today, 0) + session.getBlockedAttempts().size();
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DAILY_SESSIONS + today, dailySessions);
        editor.putLong(KEY_DAILY_TIME + today, dailyTime);
        editor.putInt(KEY_DAILY_COMPLETED + today, dailyCompleted);
        editor.putInt(KEY_DAILY_BLOCKED + today, dailyBlocked);
        
        // Update weekly stats
        int weekSessions = prefs.getInt(KEY_WEEK_SESSIONS + weekKey, 0) + 1;
        long weekTime = prefs.getLong(KEY_WEEK_TIME + weekKey, 0) + session.getActualDuration();
        editor.putInt(KEY_WEEK_SESSIONS + weekKey, weekSessions);
        editor.putLong(KEY_WEEK_TIME + weekKey, weekTime);
        
        // Update streak
        updateStreak();
        
        // Save session to history
        saveSessionToHistory(session);
        
        editor.apply();
    }
    
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
    
    private String getCurrentWeek() {
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
    
    private void updateStreak() {
        String today = getCurrentDate();
        String lastSessionDate = prefs.getString(KEY_LAST_SESSION_DATE, "");
        int currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0);
        
        if (lastSessionDate.isEmpty()) {
            // First session ever
            currentStreak = 1;
        } else if (lastSessionDate.equals(today)) {
            // Multiple sessions today, keep current streak
        } else {
            // Check if yesterday
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
                java.util.Date lastDate = sdf.parse(lastSessionDate);
                java.util.Date todayDate = sdf.parse(today);
                
                long diffInMillies = todayDate.getTime() - lastDate.getTime();
                long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);
                
                if (diffInDays == 1) {
                    // Consecutive day
                    currentStreak++;
                } else if (diffInDays > 1) {
                    // Streak broken
                    currentStreak = 1;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error calculating streak: " + e.getMessage());
                currentStreak = 1;
            }
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_SESSION_DATE, today);
        editor.putInt(KEY_CURRENT_STREAK, currentStreak);
        editor.apply();
    }
    
    private int getCurrentStreak() {
        return prefs.getInt(KEY_CURRENT_STREAK, 0);
    }
    
    private void saveSessionToHistory(AnalyticsModels.FocusSession session) {
        String today = getCurrentDate();
        String historyKey = KEY_SESSION_HISTORY + today;
        
        Set<String> sessionHistory = prefs.getStringSet(historyKey, new HashSet<>());
        Set<String> newHistory = new HashSet<>(sessionHistory);
        
        // Create session data string: startTime|duration|completed|targetDuration
        String sessionData = String.format(Locale.US, "%d|%d|%s|%d",
            session.getStartTime(),
            session.getActualDuration(),
            session.isCompleted(),
            session.getTargetDuration()
        );
        
        newHistory.add(sessionData);
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(historyKey, newHistory);
        editor.apply();
    }
    
    // Utility method to check if there's an active session
    public boolean hasActiveSession() {
        return prefs.getLong(KEY_CURRENT_SESSION_START, 0) > 0;
    }
    
    // Get current session progress
    public double getCurrentSessionProgress() {
        long startTime = prefs.getLong(KEY_CURRENT_SESSION_START, 0);
        long targetDuration = prefs.getLong(KEY_CURRENT_SESSION_TARGET, 0);
        
        if (startTime == 0 || targetDuration == 0) {
            return 0.0;
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(100.0, (double) elapsed / targetDuration * 100);
    }
} 