package com.grepguru.zenlock.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.AlarmClock;
import android.telecom.TelecomManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppUtils {

    // Get only the main default apps that should be VISIBLE on lock screen based on user preferences
    // Default apps: Phone (emergency calls), Clock (alarms), Calendar (schedules/events)
    public static Set<String> getMainDefaultApps(Context context) {
        Set<String> mainApps = new HashSet<>();
        PackageManager pm = context.getPackageManager();
        
        // Check individual preferences for each default app
        SharedPreferences preferences = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);

        // Find and add phone app if enabled by user
        if (preferences.getBoolean("allow_phone_app", true)) {
            String mainDialer = findMainDialerApp(context);
            if (mainDialer != null) {
                mainApps.add(mainDialer);
            }
        }

        // Find and add clock app if enabled by user
        if (preferences.getBoolean("allow_clock_app", true)) {
            String mainClock = findMainClockApp(context);
            if (mainClock != null) {
                mainApps.add(mainClock);
            }
        }

        // Find and add calendar app if enabled by user (replaces SMS for better productivity)
        if (preferences.getBoolean("allow_calendar_app", true)) {
            String mainCalendar = findMainCalendarApp(context);
            if (mainCalendar != null) {
                mainApps.add(mainCalendar);
            }
        }

        return mainApps;
    }

    // Get ALL packages that should be ALLOWED (including system services) based on user preferences
    public static Set<String> getAllAllowedPackages(Context context) {
        Set<String> allAllowed = new HashSet<>();
        
        // Add main default apps (already filtered by user preferences)
        allAllowed.addAll(getMainDefaultApps(context));
        
        // Get user preferences for system services
        SharedPreferences preferences = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean allowPhoneApp = preferences.getBoolean("allow_phone_app", true);
        
        // Add essential system services that must be allowed but not shown
        PackageManager pm = context.getPackageManager();
        
        // Essential call/telephony system services (only if phone app is enabled)
        if (allowPhoneApp) {
            addIfInstalled(pm, allAllowed, "com.android.server.telecom");
            addIfInstalled(pm, allAllowed, "com.android.providers.telephony");
            addIfInstalled(pm, allAllowed, "com.android.phone");
            
            // Call UI and in-call activities (needed for incoming calls)
            addIfInstalled(pm, allAllowed, "com.samsung.android.incallui"); // Samsung call screen
            addIfInstalled(pm, allAllowed, "com.android.incallui"); // Stock call screen
            addIfInstalled(pm, allAllowed, "com.sec.phone"); // Samsung phone service
        }
        
        
        // ALWAYS ALLOWED: Core system services (regardless of user preferences)
        // These are essential for the device to function properly
        
        // System UI and notification handling (critical for notifications and system function)
        addIfInstalled(pm, allAllowed, "com.android.systemui"); // System UI
        addIfInstalled(pm, allAllowed, "android"); // System process
        addIfInstalled(pm, allAllowed, "system"); // System process alternative
        
        // Launcher apps (needed for notification interactions and home button)
        addIfInstalled(pm, allAllowed, "com.sec.android.app.launcher"); // Samsung Launcher
        addIfInstalled(pm, allAllowed, "com.google.android.apps.nexuslauncher"); // Pixel Launcher
        addIfInstalled(pm, allAllowed, "com.android.launcher3"); // Stock Android Launcher
        addIfInstalled(pm, allAllowed, "com.miui.home"); // Xiaomi Launcher
        addIfInstalled(pm, allAllowed, "com.oneplus.launcher"); // OnePlus Launcher
        addIfInstalled(pm, allAllowed, "com.oppo.launcher"); // Oppo Launcher
        addIfInstalled(pm, allAllowed, "com.vivo.launcher"); // Vivo Launcher
        addIfInstalled(pm, allAllowed, "com.huawei.android.launcher"); // Huawei Launcher
        
        // Google Play Services (needed for core Android functionality)
        addIfInstalled(pm, allAllowed, "com.android.vending"); // Google Play Store
        addIfInstalled(pm, allAllowed, "com.google.android.gms"); // Google Play Services
        
        // Keyboard apps (essential for any text input in allowed apps)
        addIfInstalled(pm, allAllowed, "com.google.android.inputmethod.latin"); // Gboard
        addIfInstalled(pm, allAllowed, "com.android.inputmethod.latin"); // Stock Android Keyboard
        addIfInstalled(pm, allAllowed, "com.samsung.android.honeyboard"); // Samsung Keyboard
        addIfInstalled(pm, allAllowed, "com.touchtype.swiftkey"); // SwiftKey
        addIfInstalled(pm, allAllowed, "com.swiftkey.swiftkeyconfigurator"); // SwiftKey Configurator
        addIfInstalled(pm, allAllowed, "com.miui.securityinputmethod"); // Xiaomi Keyboard
        addIfInstalled(pm, allAllowed, "com.sohu.inputmethod.sogou"); // Sogou Keyboard
        addIfInstalled(pm, allAllowed, "com.baidu.input"); // Baidu Keyboard
        addIfInstalled(pm, allAllowed, "com.iflytek.inputmethod"); // iFlytek Keyboard
        addIfInstalled(pm, allAllowed, "com.htc.sense.ime"); // HTC Keyboard
        addIfInstalled(pm, allAllowed, "com.sonyericsson.textinput.chinese"); // Sony Keyboard
        addIfInstalled(pm, allAllowed, "com.lge.ime"); // LG Keyboard
        addIfInstalled(pm, allAllowed, "com.nuance.swype.dtc"); // Swype Keyboard
        addIfInstalled(pm, allAllowed, "com.fleksy.keyboard"); // Fleksy Keyboard
        addIfInstalled(pm, allAllowed, "com.anysoftkeyboard.api"); // AnySoftKeyboard
        addIfInstalled(pm, allAllowed, "org.pocketworkstation.pckeyboard"); // Hacker's Keyboard
        
        // Security and biometric authentication (always essential for app security)
        addIfInstalled(pm, allAllowed, "com.samsung.android.biometrics.app.setting"); // Samsung Biometrics
        addIfInstalled(pm, allAllowed, "com.samsung.android.authfw"); // Samsung Auth Framework
        addIfInstalled(pm, allAllowed, "com.samsung.android.samsungpass"); // Samsung Pass
        addIfInstalled(pm, allAllowed, "com.samsung.android.samsungpassautofill"); // Samsung Pass Autofill
        addIfInstalled(pm, allAllowed, "com.google.android.gms.auth.api.credentials"); // Google Auth
        addIfInstalled(pm, allAllowed, "com.android.keyguard"); // Lock screen security
        addIfInstalled(pm, allAllowed, "com.android.credentialmanager"); // Credential Manager
        
        // Face recognition and biometric services (always essential for security)
        addIfInstalled(pm, allAllowed, "com.miui.face"); // Xiaomi Face Recognition
        addIfInstalled(pm, allAllowed, "com.oneplus.faceunlock"); // OnePlus Face Unlock
        addIfInstalled(pm, allAllowed, "com.oppo.facerecognition"); // Oppo Face Recognition
        addIfInstalled(pm, allAllowed, "com.vivo.facerecognition"); // Vivo Face Recognition
        addIfInstalled(pm, allAllowed, "com.huawei.facerecognition"); // Huawei Face Recognition
        
        // Fingerprint and biometric authentication (always essential for security)
        addIfInstalled(pm, allAllowed, "com.android.server.biometrics"); // Biometric Service
        addIfInstalled(pm, allAllowed, "com.fingerprints.serviceext"); // Fingerprint Service
        addIfInstalled(pm, allAllowed, "com.samsung.android.biometrics"); // Samsung Biometrics Core
        addIfInstalled(pm, allAllowed, "com.google.android.apps.authenticator2"); // Google Authenticator
        
        // Security policies (always essential for system security)
        addIfInstalled(pm, allAllowed, "com.android.internal.policy"); // Security Policy
        addIfInstalled(pm, allAllowed, "com.android.server.policy"); // Server Policy
        
        return allAllowed;
    }

    // Legacy method - now calls getMainDefaultApps for backward compatibility
    public static Set<String> getDefaultApps(Context context) {
        return getMainDefaultApps(context);
    }

    public static String findMainDialerApp(Context context) {
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
    
    public static String findMainCalendarApp(Context context) {
        PackageManager pm = context.getPackageManager();
        
        // Try system method first
        try {
            Intent calendarIntent = new Intent(Intent.ACTION_INSERT);
            calendarIntent.setType("vnd.android.cursor.dir/event");
            ResolveInfo resolveInfo = pm.resolveActivity(calendarIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String calendarPackage = resolveInfo.activityInfo.packageName;
                if (isPackageInstalled(pm, calendarPackage) && !calendarPackage.equals("android")) {
                    return calendarPackage;
                }
            }
        } catch (Exception e) {
            // Fallback to hardcoded list
        }
        
        // Fallback: Try main calendar apps (only the primary ones users see)
        String[] mainCalendarApps = {
            "com.google.android.calendar",       // Google Calendar
            "com.samsung.android.calendar",      // Samsung Calendar
            "com.android.calendar",              // Stock Android
            "com.miui.calendar",                 // Xiaomi Calendar
            "com.oneplus.calendar",              // OnePlus Calendar
            "com.oppo.calendar",                 // Oppo Calendar
            "com.vivo.calendar",                 // Vivo Calendar
            "com.huawei.calendar"                // Huawei Calendar
        };
        
        for (String pkg : mainCalendarApps) {
            if (isPackageInstalled(pm, pkg)) {
                return pkg;
            }
        }
        
        return null;
    }
    
    public static String findMainClockApp(Context context) {
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
