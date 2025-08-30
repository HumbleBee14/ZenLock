package com.grepguru.focuslock.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.AlarmClock;
import android.telecom.TelecomManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppUtils {

    public static Set<String> getDefaultApps(Context context) {
        Set<String> defaultApps = new HashSet<>();
        PackageManager pm = context.getPackageManager();

        // Find default dialer app using system method
        try {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                String defaultDialer = telecomManager.getDefaultDialerPackage();
                if (defaultDialer != null && isPackageInstalled(pm, defaultDialer)) {
                    defaultApps.add(defaultDialer);
                }
            }
        } catch (Exception e) {
            // Fallback to hardcoded list if system method fails
        }
        
        // Fallback: Try common dialer apps if system method didn't work
        if (defaultApps.isEmpty()) {
            String[] dialerApps = {"com.android.phone", "com.google.android.dialer", "com.samsung.android.dialer", 
                                   "com.android.dialer", "com.htc.android.phone", "com.sonyericsson.android.phone"};
            for (String pkg : dialerApps) {
                if (isPackageInstalled(pm, pkg)) {
                    defaultApps.add(pkg);
                    break;
                }
            }
        }

        // Find default SMS app using system method
        try {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            smsIntent.setData(Uri.parse("sms:"));
            ResolveInfo resolveInfo = pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String smsPackage = resolveInfo.activityInfo.packageName;
                if (isPackageInstalled(pm, smsPackage)) {
                    defaultApps.add(smsPackage);
                }
            }
        } catch (Exception e) {
            // Fallback to hardcoded list
            String[] smsApps = {"com.android.mms", "com.google.android.apps.messaging", "com.samsung.android.messaging",
                                "com.android.messaging", "com.htc.android.mms"};
            for (String pkg : smsApps) {
                if (isPackageInstalled(pm, pkg)) {
                    defaultApps.add(pkg);
                    break;
                }
            }
        }

        // Find clock/alarm app using system method
        try {
            Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
            List<ResolveInfo> alarmApps = pm.queryIntentActivities(alarmIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (!alarmApps.isEmpty()) {
                String clockPackage = alarmApps.get(0).activityInfo.packageName;
                if (isPackageInstalled(pm, clockPackage)) {
                    defaultApps.add(clockPackage);
                }
            }
        } catch (Exception e) {
            // Fallback to hardcoded list
            String[] clockApps = {"com.android.deskclock", "com.google.android.deskclock", "com.sec.android.app.clockpackage",
                                  "com.htc.android.worldclock", "com.sonyericsson.organizer"};
            for (String pkg : clockApps) {
                if (isPackageInstalled(pm, pkg)) {
                    defaultApps.add(pkg);
                    break;
                }
            }
        }

        return defaultApps;
    }


    private static boolean isPackageInstalled(PackageManager pm, String packageName) {
        try {
            pm.getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
