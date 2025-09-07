package com.grepguru.zenlock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralized whitelist management to prevent code duplication
 * and ensure consistent whitelist logic across the app
 */
public class WhitelistManager {
    
    private static final String TAG = "WhitelistManager";
    
    /**
     * Check if an app is whitelisted (centralized logic)
     * @param context The context
     * @param packageName The package name to check
     * @return True if the app is whitelisted, false otherwise
     */
    public static boolean isAppWhitelisted(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        // Always allow the ZenLock app itself
        if ("com.grepguru.zenlock".equals(packageName)) {
            return true;
        }
        
        // SECURITY CHECK: Block known security risk packages
        if (isSecurityRisk(packageName)) {
            return false;
        }
        
        // Allow essential system packages (SECURITY: Removed settings and launcher)
        String[] essentialSystemPackages = {
            "com.android.systemui",           // System UI (status bar, navigation, etc.)
            "com.android.keyguard",           // System lock screen
            "android",                        // Core Android system
            "com.android.phone",              // Phone app (for emergency calls)
            "com.android.incallui",           // In-call UI
            "com.android.dialer",             // Dialer app
            "com.android.emergency",          // Emergency services
            "com.android.camera2",            // Camera (for emergency photos)
            "com.android.camera",             // Camera (alternative)
            "com.google.android.gms",         // Google Play Services
            "com.google.android.gsf"          // Google Services Framework
            // REMOVED: "com.android.settings" - Security risk (allows force stop)
            // REMOVED: "com.sec.android.app.launcher" - Security risk (allows home screen access)
            // REMOVED: All launcher packages - Security risk (allows home screen access)
        };
        
        for (String systemPackage : essentialSystemPackages) {
            if (systemPackage.equals(packageName)) {
                Log.d(TAG, "Essential system package allowed: " + packageName);
                return true;
            }
        }
        
        // Check user whitelisted apps
        SharedPreferences preferences = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        
        // Get ALL allowed packages (including system services for in-app activities)
        Set<String> allAllowedApps = new HashSet<>(whitelistedApps);
        allAllowedApps.addAll(AppUtils.getAllAllowedPackages(context));
        
        boolean isWhitelisted = allAllowedApps.contains(packageName);
        if (isWhitelisted) {
            Log.d(TAG, "User whitelisted app allowed: " + packageName);
        }
        
        return isWhitelisted;
    }
    
    /**
     * Get a list of all whitelisted packages for debugging
     * @param context The context
     * @return Set of all whitelisted package names
     */
    public static Set<String> getAllWhitelistedPackages(Context context) {
        Set<String> allWhitelisted = new HashSet<>();
        
        // Add essential system packages
        String[] essentialSystemPackages = {
            "com.android.systemui",
            "com.android.keyguard", 
            "android",
            "com.android.phone",
            "com.android.incallui",
            "com.android.dialer",
            "com.android.emergency",
            "com.android.camera2",
            "com.android.camera",
            "com.google.android.gms",
            "com.google.android.gsf"
        };
        
        for (String systemPackage : essentialSystemPackages) {
            allWhitelisted.add(systemPackage);
        }
        
        // Add user whitelisted apps
        SharedPreferences preferences = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        allWhitelisted.addAll(whitelistedApps);
        
        // Add AppUtils allowed packages
        allWhitelisted.addAll(AppUtils.getAllAllowedPackages(context));
        
        return allWhitelisted;
    }
    
    /**
     * Log all whitelisted packages for debugging
     * @param context The context
     */
    public static void logAllWhitelistedPackages(Context context) {
        Set<String> allWhitelisted = getAllWhitelistedPackages(context);
        Log.d(TAG, "All whitelisted packages (" + allWhitelisted.size() + "): " + allWhitelisted);
    }
    
    /**
     * Check if a package is a security risk (settings, launcher, etc.)
     * @param packageName The package name to check
     * @return True if the package is a security risk
     */
    public static boolean isSecurityRisk(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        String[] securityRiskPackages = {
            "com.android.settings",           // Allows force stop
            "com.sec.android.app.launcher",   // Samsung launcher
            "com.android.launcher",           // Stock launcher
            "com.android.launcher2",          // Legacy launcher
            "com.android.launcher3",          // AOSP launcher
            "com.google.android.launcher",    // Google launcher
            "com.samsung.android.launcher",   // Samsung launcher
            "com.miui.home",                  // MIUI launcher
            "com.oneplus.launcher",           // OnePlus launcher
            "com.huawei.android.launcher",    // Huawei launcher
            "com.oppo.launcher",              // OPPO launcher
            "com.vivo.launcher",              // Vivo launcher
            "com.realme.launcher"             // Realme launcher
        };
        
        for (String riskPackage : securityRiskPackages) {
            if (riskPackage.equals(packageName)) {
                Log.w(TAG, "SECURITY RISK: Package " + packageName + " is blocked for security reasons");
                return true;
            }
        }
        
        return false;
    }
}
