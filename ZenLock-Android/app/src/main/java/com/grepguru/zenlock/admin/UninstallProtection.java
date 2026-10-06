package com.grepguru.zenlock.admin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ActivityInfo;
import java.util.HashSet;
import java.util.Set;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import com.grepguru.zenlock.R;

public final class UninstallProtection {
    private UninstallProtection() {}

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

    public static boolean isManagementWindow(Context context, String packageName, String className) {
        Set<String> settingsPackages = new HashSet<>();
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(
                new Intent(Settings.ACTION_SETTINGS), 0)) {
            if (isSystemActivity(info)) settingsPackages.add(info.activityInfo.packageName);
        }
        Intent[] adminIntents = {
                activationIntent(context), new Intent("android.settings.DEVICE_ADMIN_SETTINGS")
        };
        for (Intent intent : adminIntents) {
            for (ResolveInfo info : context.getPackageManager().queryIntentActivities(intent, 0)) {
                if (!isSystemActivity(info)) continue;
                settingsPackages.add(info.activityInfo.packageName);
                if (matchesActivity(info.activityInfo, packageName, className)) return true;
            }
        }
        Intent[] uninstallIntents = {
                new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + context.getPackageName())),
                new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + context.getPackageName()))
        };
        for (Intent intent : uninstallIntents) {
            for (ResolveInfo info : context.getPackageManager().queryIntentActivities(intent, 0)) {
                if (!isSystemActivity(info) || !packageName.equals(info.activityInfo.packageName)) continue;
                // Dedicated system installers can redirect to a progress activity or dialog.
                // Settings remains restricted to its resolved management activities.
                if (!settingsPackages.contains(packageName)
                        || matchesActivity(info.activityInfo, packageName, className)) return true;
            }
        }
        return false;
    }

    private static boolean isSystemActivity(ResolveInfo info) {
        return info.activityInfo != null && info.activityInfo.applicationInfo != null
                && (info.activityInfo.applicationInfo.flags
                    & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private static boolean matchesActivity(ActivityInfo info, String packageName, String className) {
        return packageName.equals(info.packageName)
                && className.equals(info.name);
    }
}
