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

    // Get only the main default apps that should be VISIBLE on lock screen
    public static Set<String> getMainDefaultApps(Context context) {
        Set<String> mainApps = new HashSet<>();
        PackageManager pm = context.getPackageManager();

        // Find and add only ONE main dialer app
        String mainDialer = findMainDialerApp(context);
        if (mainDialer != null) {
            mainApps.add(mainDialer);
        }

        // Find and add only ONE main SMS app  
        String mainSms = findMainSmsApp(context);
        if (mainSms != null) {
            mainApps.add(mainSms);
        }

        // Find and add only ONE main clock app
        String mainClock = findMainClockApp(context);
        if (mainClock != null) {
            mainApps.add(mainClock);
        }

        return mainApps;
    }

    // Get ALL packages that should be ALLOWED (including system services)
    public static Set<String> getAllAllowedPackages(Context context) {
        Set<String> allAllowed = new HashSet<>();
        
        // Add main default apps
        allAllowed.addAll(getMainDefaultApps(context));
        
        // Add essential system services that must be allowed but not shown
        PackageManager pm = context.getPackageManager();
        
        // Essential call/telephony system services
        addIfInstalled(pm, allAllowed, "com.android.server.telecom");
        addIfInstalled(pm, allAllowed, "com.android.providers.telephony");
        addIfInstalled(pm, allAllowed, "com.android.phone");
        
        // Call UI and in-call activities (the ones that were working before)
        addIfInstalled(pm, allAllowed, "com.samsung.android.incallui"); // Samsung call screen
        addIfInstalled(pm, allAllowed, "com.android.incallui"); // Stock call screen
        addIfInstalled(pm, allAllowed, "com.sec.phone"); // Samsung phone service
        
        // System UI and notification handling (for reopening from notifications)
        addIfInstalled(pm, allAllowed, "com.android.systemui"); // System UI (critical for notifications)
        addIfInstalled(pm, allAllowed, "android"); // System process
        addIfInstalled(pm, allAllowed, "system"); // System process alternative
        
        // Launcher apps (needed for notification interactions)
        addIfInstalled(pm, allAllowed, "com.sec.android.app.launcher"); // Samsung Launcher
        addIfInstalled(pm, allAllowed, "com.google.android.apps.nexuslauncher"); // Pixel Launcher
        addIfInstalled(pm, allAllowed, "com.android.launcher3"); // Stock Android Launcher
        addIfInstalled(pm, allAllowed, "com.miui.home"); // Xiaomi Launcher
        addIfInstalled(pm, allAllowed, "com.oneplus.launcher"); // OnePlus Launcher
        addIfInstalled(pm, allAllowed, "com.oppo.launcher"); // Oppo Launcher
        addIfInstalled(pm, allAllowed, "com.vivo.launcher"); // Vivo Launcher
        addIfInstalled(pm, allAllowed, "com.huawei.android.launcher"); // Huawei Launcher
        
        // Google Play Services (needed for some SMS/messaging features)
        addIfInstalled(pm, allAllowed, "com.android.vending"); // Google Play Store
        addIfInstalled(pm, allAllowed, "com.google.android.gms"); // Google Play Services
        
        // Essential SMS/MMS system services
        addIfInstalled(pm, allAllowed, "com.android.mms.service");
        addIfInstalled(pm, allAllowed, "com.android.providers.sms");
        
        return allAllowed;
    }

    // Legacy method - now calls getMainDefaultApps for backward compatibility
    public static Set<String> getDefaultApps(Context context) {
        return getMainDefaultApps(context);
    }

    private static String findMainDialerApp(Context context) {
        PackageManager pm = context.getPackageManager();
        
        // Try system method first
        try {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                String defaultDialer = telecomManager.getDefaultDialerPackage();
                if (defaultDialer != null && isPackageInstalled(pm, defaultDialer)) {
                    return defaultDialer;
                }
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        // Fallback: Try main dialer apps (only the primary ones users see)
        String[] mainDialerApps = {
            "com.google.android.dialer",    // Google Phone
            "com.samsung.android.dialer",   // Samsung Phone
            "com.android.dialer",           // Stock Android
            "com.miui.contacts",            // Xiaomi Contacts/Dialer
            "com.oneplus.dialer",           // OnePlus Dialer
            "com.oppo.dialer",              // Oppo Dialer
            "com.vivo.dialer",              // Vivo Dialer
            "com.huawei.contacts"           // Huawei Contacts
        };
        
        for (String pkg : mainDialerApps) {
            if (isPackageInstalled(pm, pkg)) {
                return pkg; // Return the first found
            }
        }
        
        return null;
    }
    
    private static String findMainSmsApp(Context context) {
        PackageManager pm = context.getPackageManager();
        
        // Try system method first
        try {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            smsIntent.setData(Uri.parse("sms:"));
            ResolveInfo resolveInfo = pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String smsPackage = resolveInfo.activityInfo.packageName;
                if (isPackageInstalled(pm, smsPackage)) {
                    return smsPackage;
                }
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        // Fallback: Try main SMS apps (only the primary ones users see)
        String[] mainSmsApps = {
            "com.google.android.apps.messaging", // Google Messages
            "com.samsung.android.messaging",      // Samsung Messages
            "com.android.mms",                    // Stock Android
            "com.miui.mms",                       // Xiaomi Messages
            "com.oneplus.mms",                    // OnePlus Messages
            "com.oppo.mms",                       // Oppo Messages
            "com.vivo.mms",                       // Vivo Messages
            "com.huawei.mms"                      // Huawei Messages
        };
        
        for (String pkg : mainSmsApps) {
            if (isPackageInstalled(pm, pkg)) {
                return pkg; // Return the first found
            }
        }
        
        return null;
    }
    
    private static String findMainClockApp(Context context) {
        PackageManager pm = context.getPackageManager();
        
        // Try system method first
        try {
            Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
            List<ResolveInfo> alarmApps = pm.queryIntentActivities(alarmIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (!alarmApps.isEmpty()) {
                String clockPackage = alarmApps.get(0).activityInfo.packageName;
                if (isPackageInstalled(pm, clockPackage)) {
                    return clockPackage;
                }
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        // Fallback: Try main clock apps (only the primary ones users see)
        String[] mainClockApps = {
            "com.google.android.deskclock",       // Google Clock
            "com.sec.android.app.clockpackage",   // Samsung Clock
            "com.android.deskclock",              // Stock Android
            "com.miui.clock",                     // Xiaomi Clock
            "com.oneplus.deskclock",              // OnePlus Clock
            "com.oppo.alarmclock",                // Oppo Clock
            "com.vivo.alarmclock",                // Vivo Clock
            "com.huawei.deskclock"                // Huawei Clock
        };
        
        for (String pkg : mainClockApps) {
            if (isPackageInstalled(pm, pkg)) {
                return pkg; // Return the first found
            }
        }
        
        return null;
    }
    
    // Helper method to add package if installed
    private static void addIfInstalled(PackageManager pm, Set<String> defaultApps, String packageName) {
        if (isPackageInstalled(pm, packageName)) {
            defaultApps.add(packageName);
        }
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
