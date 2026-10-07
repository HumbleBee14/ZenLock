package com.grepguru.zenlock.admin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import com.grepguru.zenlock.R;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class UninstallProtection {
    private UninstallProtection() {}

    private static final class ResolvedWindows {
        final Set<String> settingsPackages = new HashSet<>();
        final Set<ComponentName> adminActivities = new HashSet<>();
        final Map<String, Set<String>> uninstallActivities = new HashMap<>();
    }

    private static volatile ResolvedWindows resolved;

    public static ComponentName component(Context context) {
        return new ComponentName(context, UninstallProtectionReceiver.class);
    }

    public static boolean isEnabled(Context context) {
        DevicePolicyManager manager = context.getSystemService(DevicePolicyManager.class);
        return manager != null && manager.isAdminActive(component(context));
    }

    public static Intent activationIntent(Context context) {
        return new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.uninstall_protection_explanation));
    }

    public static void disable(Context context) {
        DevicePolicyManager manager = context.getSystemService(DevicePolicyManager.class);
        if (manager != null && manager.isAdminActive(component(context))) {
            manager.removeActiveAdmin(component(context));
        }
    }

    public static void refresh() {
        resolved = null;
    }

    public static boolean isManagementWindow(Context context, String packageName, String className) {
        if (!isEnabled(context)) return false;
        ResolvedWindows windows = resolved;
        if (windows == null) {
            windows = resolve(context);
            resolved = windows;
        }
        if (windows.adminActivities.contains(new ComponentName(packageName, className))) return true;
        Set<String> uninstall = windows.uninstallActivities.get(packageName);
        if (uninstall == null) return false;
        return !windows.settingsPackages.contains(packageName) || uninstall.contains(className);
    }

    private static ResolvedWindows resolve(Context context) {
        ResolvedWindows windows = new ResolvedWindows();
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo info : pm.queryIntentActivities(new Intent(Settings.ACTION_SETTINGS), 0)) {
            if (isSystemActivity(info)) windows.settingsPackages.add(info.activityInfo.packageName);
        }
        Intent[] adminIntents = {
                activationIntent(context), new Intent("android.settings.DEVICE_ADMIN_SETTINGS")
        };
        for (Intent intent : adminIntents) {
            for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
                if (!isSystemActivity(info)) continue;
                windows.settingsPackages.add(info.activityInfo.packageName);
                windows.adminActivities.add(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            }
        }
        Uri self = Uri.parse("package:" + context.getPackageName());
        Intent[] uninstallIntents = {
                new Intent(Intent.ACTION_DELETE, self), new Intent(Intent.ACTION_UNINSTALL_PACKAGE, self)
        };
        for (Intent intent : uninstallIntents) {
            for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
                if (!isSystemActivity(info)) continue;
                Set<String> activities = windows.uninstallActivities.get(info.activityInfo.packageName);
                if (activities == null) {
                    activities = new HashSet<>();
                    windows.uninstallActivities.put(info.activityInfo.packageName, activities);
                }
                activities.add(info.activityInfo.name);
            }
        }
        return windows;
    }

    private static boolean isSystemActivity(ResolveInfo info) {
        return info.activityInfo != null && info.activityInfo.applicationInfo != null
                && (info.activityInfo.applicationInfo.flags
                    & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }
}
