package com.grepguru.zenlock.utils;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Set;

/**
 * MobileUsageTracker - Tracks mobile usage using UsageStatsManager
 * Provides methods to get total screen time and app usage statistics
 */
public class MobileUsageTracker {
    
    private static final String TAG = "MobileUsageTracker";
    private static final boolean DEBUG_LOGS = false; // Set to true for debugging
    
    private Context context;
    private UsageStatsManager usageStatsManager;
    
    public MobileUsageTracker(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }
    
    // ---- Modular utilities for time ranges + total usage ----
    public static long[] getMonthTimestamps(int year, int month) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        // Start of month
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        // End of month (inclusive)
        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endTime = calendar.getTimeInMillis();

        return new long[]{startTime, endTime};
    }

    /**
     * Returns start/end timestamps for the provided calendar's day (local TZ).
     */
    public static long[] getDayTimestamps(Calendar day) {
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();
        return new long[]{start, end};
    }

    /**
     * Returns start-of-week (Mon 00:00) and end-of-week (Sun 23:59:59.999) for the week containing 'anyDay'.
     */
    public static long[] getWeekTimestamps(Calendar anyDay) {
        Calendar cal = (Calendar) anyDay.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_YEAR, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();
        return new long[]{start, end};
    }

    /**
     * Returns start-of-week (Mon 00:00) to now for the current week.
     */
    public static long[] getThisWeekSoFarTimestamps() {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        Calendar start = (Calendar) now.clone();
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return new long[]{start.getTimeInMillis(), System.currentTimeMillis()};
    }

    public static long getTotalPhoneUsage(Context context, long startTime, long endTime) {
        long totalTimeInMillis = 0L;
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) return 0L;

        // Build exclusion sets
        Set<String> keyboardPackages = getEnabledKeyboardPackages(context);
        Set<String> launcherPackages = getLauncherPackages(context);
        PackageManager pm = context.getPackageManager();
        String ourPackage = context.getPackageName();

        Map<String, UsageStats> usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
        if (usageStatsMap != null) {
            for (Map.Entry<String, UsageStats> entry : usageStatsMap.entrySet()) {
                String packageName = entry.getKey();
                UsageStats usageStats = entry.getValue();
                if (usageStats == null || packageName == null) continue;

                if (shouldExcludePackage(packageName, context, pm, keyboardPackages, launcherPackages, ourPackage)) continue;

                totalTimeInMillis += usageStats.getTotalTimeInForeground();
            }
        }
        return totalTimeInMillis;
    }

    /** Centralized filtering check used across the class. */
    private static boolean shouldExcludePackage(
            String packageName,
            Context context,
            PackageManager pm,
            Set<String> keyboardPackages,
            Set<String> launcherPackages,
            String ourPackage
    ) {
        if (packageName == null) return true;
        if (packageName.equals(ourPackage)) return true; // exclude our app
        if (packageName.startsWith("com.android.systemui")) return true;
        if (packageName.equals("com.google.android.gms")) return true; // Play Services
        if (packageName.equals("android")) return true; // framework
        if (keyboardPackages.contains(packageName)) return true;
        if (launcherPackages.contains(packageName)) return true;
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            boolean isSystem = (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (isSystem) return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return true;
        }
        return false;
    }

    private static Set<String> getEnabledKeyboardPackages(Context context) {
        Set<String> set = new HashSet<>();
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                List<InputMethodInfo> list = imm.getEnabledInputMethodList();
                if (list != null) {
                    for (InputMethodInfo imi : list) {
                        if (imi != null && imi.getPackageName() != null) {
                            set.add(imi.getPackageName());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        // Common fallbacks
        set.add("com.google.android.inputmethod.latin");
        set.add("com.samsung.android.honeyboard");
        set.add("com.touchtype"); // SwiftKey
        set.add("com.miui.inputmethod");
        return set;
    }

    private static Set<String> getLauncherPackages(Context context) {
        Set<String> set = new HashSet<>();
        try {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
            if (infos != null) {
                for (ResolveInfo ri : infos) {
                    if (ri != null && ri.activityInfo != null) {
                        set.add(ri.activityInfo.packageName);
                    }
                }
            }
        } catch (Exception ignored) {}
        // Common launcher fallbacks
        set.add("com.android.launcher3");
        set.add("com.google.android.apps.nexuslauncher"); // Pixel Launcher
        set.add("com.teslacoilsw.launcher"); // Nova
        set.add("com.mi.android.globallauncher");
        set.add("com.sec.android.app.launcher"); // Samsung
        return set;
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
            
            if (DEBUG_LOGS) {
                Log.d(TAG, "Usage stats permission mode: " + modeToString(mode));
            }
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
     * Get this week's mobile usage time
     */
    public long getThisWeekMobileUsage() {
        if (!hasUsageStatsPermission()) return 0;
        try {
            long[] range = getThisWeekSoFarTimestamps();
            return getTotalPhoneUsage(context, range[0], range[1]);
        } catch (Exception e) {
            Log.e(TAG, "Error getting this week's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get last week's mobile usage time
     */
    public long getLastWeekMobileUsage() {
        if (!hasUsageStatsPermission()) return 0;
        try {
            Calendar anyDayLastWeek = Calendar.getInstance(TimeZone.getDefault());
            anyDayLastWeek.add(Calendar.WEEK_OF_YEAR, -1);
            long[] range = getWeekTimestamps(anyDayLastWeek);
            return getTotalPhoneUsage(context, range[0], range[1]);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last week's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get this month's mobile usage time
     */
    public long getThisMonthMobileUsage() {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted");
            return 0;
        }
        
        try {
            // Use modular utilities: start = first day, end = now (so far)
            Calendar now = Calendar.getInstance();
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH);
            long[] range = getMonthTimestamps(year, month);
            long startTime = range[0];
            long endTime = System.currentTimeMillis(); // so far

            return getTotalPhoneUsage(context, startTime, endTime);
        } catch (Exception e) {
            Log.e(TAG, "Error getting this month's mobile usage", e);
            return 0;
        }
    }
    
    /**
     * Get last month's mobile usage time
     */
    public long getLastMonthMobileUsage() {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted");
            return 0;
        }
        
        try {
            // Compute last month year/month, then use modular utilities
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH); // 0-indexed
            long[] range = getMonthTimestamps(year, month);
            long startTime = range[0];
            long endTime = range[1];

            return getTotalPhoneUsage(context, startTime, endTime);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last month's mobile usage", e);
            return 0;
        }
    }
    
    // Legacy getMobileUsageForPeriod() removed in favor of getTotalPhoneUsage() with filtering

    /**
     * Get total mobile usage for today in milliseconds
     */
    public long getTodayMobileUsage() {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted");
            return 0;
        }
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            long start = getDayTimestamps(cal)[0];
            long end = System.currentTimeMillis();
            return getTotalPhoneUsage(context, start, end);
        } catch (Exception e) {
            Log.e(TAG, "Error getting today's mobile usage", e);
            return 0;
        }
    }

    /**
     * Get yesterday's total mobile usage in milliseconds
     */
    public long getYesterdayMobileUsage() {
        if (!hasUsageStatsPermission()) return 0;
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.add(Calendar.DAY_OF_YEAR, -1);
            long[] range = getDayTimestamps(cal);
            return getTotalPhoneUsage(context, range[0], range[1]);
        } catch (Exception e) {
            Log.e(TAG, "Error getting yesterday's mobile usage", e);
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

            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.setTime(targetDate);
            long[] range = getDayTimestamps(cal);
            long total = getTotalPhoneUsage(context, range[0], range[1]);
            if (DEBUG_LOGS) Log.d(TAG, "Mobile usage for " + date + ": " + formatDuration(total));
            return total;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mobile usage for date: " + date, e);
            return 0;
        }
    }
    
    /**
     * Get mobile usage for the current week in milliseconds
     */
    public long getCurrentWeekMobileUsage() {
    return getThisWeekMobileUsage();
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

        // Apply same filtering as totals
        Set<String> keyboardPackages = getEnabledKeyboardPackages(context);
        Set<String> launcherPackages = getLauncherPackages(context);
        PackageManager pm = context.getPackageManager();
        String ourPackage = context.getPackageName();

        usageStatsList.removeIf(stats -> stats == null ||
            shouldExcludePackage(stats.getPackageName(), context, pm, keyboardPackages, launcherPackages, ourPackage));

            // Sort by usage time and return top apps
            usageStatsList.sort((a, b) -> Long.compare(b.getTotalTimeInForeground(), a.getTotalTimeInForeground()));

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
    
    // Removed detailed debugging report for production

    // Debug-only helpers removed for production build
    
    // Removed UI name simplifier used only in debug output
    
    /**
     * Calculate usage percentage saved (caller provides focus time)
     */
    public double calculateUsageSavedPercentage(long totalMobileUsage, long focusTime) {
        if (totalMobileUsage == 0) return 0;
        return (double) focusTime / totalMobileUsage * 100;
    }
}
