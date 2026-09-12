package com.grepguru.zenlock.utils;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TopAppsUsage {

    public static final int TODAY = 0;
    public static final int WEEK = 1;
    public static final int MONTH = 2;

    public static final class Entry {
        public final String packageName;
        public final String label;
        public final Drawable icon;
        public final long timeMs;

        Entry(String packageName, String label, Drawable icon, long timeMs) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.timeMs = timeMs;
        }
    }

    private TopAppsUsage() {}

    public static long[] range(int period) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        if (period == WEEK) {
            int daysFromMonday = (start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
            start.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        } else if (period == MONTH) {
            start.set(Calendar.DAY_OF_MONTH, 1);
        }
        return new long[]{start.getTimeInMillis(), System.currentTimeMillis()};
    }

    public static List<Entry> top(Context context, int period, int limit) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return Collections.emptyList();
        long[] range = range(period);
        Map<String, UsageStats> stats = manager.queryAndAggregateUsageStats(range[0], range[1]);
        if (stats == null || stats.isEmpty()) return Collections.emptyList();

        PackageManager pm = context.getPackageManager();
        Set<String> excluded = excludedPackages(context, pm);
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, UsageStats> item : stats.entrySet()) {
            String packageName = item.getKey();
            UsageStats usage = item.getValue();
            if (packageName == null || usage == null || usage.getTotalTimeInForeground() < 60_000L) continue;
            if (excluded.contains(packageName)) continue;
            if (pm.getLaunchIntentForPackage(packageName) == null) continue;
            try {
                CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
                Drawable icon = pm.getApplicationIcon(packageName);
                entries.add(new Entry(packageName, label.toString(), icon, usage.getTotalTimeInForeground()));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.timeMs, a.timeMs));
        return entries.size() > limit ? new ArrayList<>(entries.subList(0, limit)) : entries;
    }

    private static Set<String> excludedPackages(Context context, PackageManager pm) {
        Set<String> set = new HashSet<>();
        set.add(context.getPackageName());
        set.add("android");
        set.add("com.google.android.gms");
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo info : pm.queryIntentActivities(home, 0)) {
            if (info.activityInfo != null) set.add(info.activityInfo.packageName);
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo info : imm.getEnabledInputMethodList()) set.add(info.getPackageName());
        }
        return set;
    }
}
