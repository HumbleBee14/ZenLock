package com.grepguru.zenlock.utils;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MobileUsageTracker - Tracks mobile usage using UsageStatsManager
 * Provides methods to get total screen time and app usage statistics
 */
public class MobileUsageTracker {
    
    private static final String TAG = "MobileUsageTracker";
    
    private Context context;
    private UsageStatsManager usageStatsManager;
    
    public MobileUsageTracker(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }
    
    /**
     * Check if the app has usage access permission
     */
    public boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false; // UsageStatsManager not available before Lollipop
        }
        
        try {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager == null) {
                Log.e(TAG, "AppOpsManager is null, cannot check usage stats permission.");
                return false;
            }
            
            int mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (API 29) and above, use unsafeCheckOpNoThrow
                mode = appOpsManager.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.getPackageName()
                );
            } else {
                // For versions below Q, use the deprecated method with suppression
                @SuppressWarnings("deprecation")
                int deprecatedMode = appOpsManager.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.getPackageName()
                );
                mode = deprecatedMode;
            }
            
            Log.d(TAG, "Usage stats permission mode: " + modeToString(mode));
            return mode == AppOpsManager.MODE_ALLOWED;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking usage stats permission", e);
            return false;
        }
    }
    
    /**
     * Helper method to convert mode to string for logging
     */
    private String modeToString(int mode) {
        switch (mode) {
            case AppOpsManager.MODE_ALLOWED:
                return "MODE_ALLOWED";
            case AppOpsManager.MODE_ERRORED:
                return "MODE_ERRORED";
            case AppOpsManager.MODE_IGNORED:
                return "MODE_IGNORED";
            case AppOpsManager.MODE_DEFAULT:
                return "MODE_DEFAULT";
            default:
                return "Unknown mode: " + mode;
        }
    }
    
    /**
     * Open settings page for granting usage access permission
     */
    public void requestUsageStatsPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening usage access settings", e);
        }
    }
    
    /**
     * Get total mobile usage for today in milliseconds
     */
    public long getTodayMobileUsage() {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted");
            return 0;
        }
        
        try {
            // Get usage stats for today
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
            
            long totalUsageTime = 0;
            for (UsageStats usageStats : usageStatsList) {
                // Exclude our own app from the calculation
                if (!usageStats.getPackageName().equals(context.getPackageName())) {
                    totalUsageTime += usageStats.getTotalTimeInForeground();
                }
            }
            
            Log.d(TAG, "Today's mobile usage: " + formatDuration(totalUsageTime));
            return totalUsageTime;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting today's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get mobile usage for a specific date in milliseconds
     */
    public long getMobileUsageForDate(String date) {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted");
            return 0;
        }
        
        try {
            // Parse date and set time range
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date targetDate = sdf.parse(date);
            if (targetDate == null) return 0;
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(targetDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();
            
            cal.add(Calendar.DAY_OF_YEAR, 1);
            long endTime = cal.getTimeInMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
            
            long totalUsageTime = 0;
            for (UsageStats usageStats : usageStatsList) {
                // Exclude our own app from the calculation
                if (!usageStats.getPackageName().equals(context.getPackageName())) {
                    totalUsageTime += usageStats.getTotalTimeInForeground();
                }
            }
            
            Log.d(TAG, "Mobile usage for " + date + ": " + formatDuration(totalUsageTime));
            return totalUsageTime;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mobile usage for date: " + date, e);
            return 0;
        }
    }
    
    /**
     * Get mobile usage for the current week in milliseconds
     */
    public long getCurrentWeekMobileUsage() {
        if (!hasUsageStatsPermission()) {
            return 0;
        }
        
        try {
            // Get start of current week (Monday)
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime);
            
            long totalUsageTime = 0;
            for (UsageStats usageStats : usageStatsList) {
                if (!usageStats.getPackageName().equals(context.getPackageName())) {
                    totalUsageTime += usageStats.getTotalTimeInForeground();
                }
            }
            
            return totalUsageTime;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting current week mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get today's mobile usage and return it (caller handles saving)
     */
    public long updateTodayMobileUsage() {
        return getTodayMobileUsage();
    }
    
    /**
     * Get mobile usage for a specific date and return it (caller handles saving)
     */
    public long updateMobileUsageForDate(String date) {
        return getMobileUsageForDate(date);
    }
    
    /**
     * Get most used apps today (top 5)
     */
    public List<UsageStats> getTopAppsToday(int limit) {
        if (!hasUsageStatsPermission()) {
            return null;
        }
        
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
            
            // Sort by usage time and return top apps
            usageStatsList.sort((a, b) -> Long.compare(b.getTotalTimeInForeground(), a.getTotalTimeInForeground()));
            
            // Remove our own app and return top apps
            usageStatsList.removeIf(stats -> stats.getPackageName().equals(context.getPackageName()));
            
            return usageStatsList.subList(0, Math.min(limit, usageStatsList.size()));
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting top apps", e);
            return null;
        }
    }
    
    /**
     * Check if usage stats permission is available on this device
     */
    public boolean isUsageStatsAvailable() {
        return usageStatsManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }
    
    /**
     * Format duration in milliseconds to readable string
     */
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
    
    /**
     * Get formatted mobile usage for display
     */
    public String getFormattedTodayUsage() {
        long usage = getTodayMobileUsage();
        return formatDuration(usage);
    }
    
    /**
     * Calculate usage percentage saved (caller provides focus time)
     */
    public double calculateUsageSavedPercentage(long totalMobileUsage, long focusTime) {
        if (totalMobileUsage == 0) return 0;
        return (double) focusTime / totalMobileUsage * 100;
    }
}
