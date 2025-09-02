package com.grepguru.zenlock.utils;

import android.util.Log;
import com.grepguru.zenlock.BuildConfig;

/**
 * Secure logging utility that automatically disables debug logs in release builds
 * and provides safe logging methods for sensitive data.
 */
public class SecureLog {
    
    /**
     * Debug log - only shows in debug builds
     */
    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG_LOGGING) {
            Log.d(tag, message);
        }
    }
    
    /**
     * Info log - shows in all builds but should not contain sensitive data
     */
    public static void i(String tag, String message) {
        Log.i(tag, message);
    }
    
    /**
     * Warning log - shows in all builds but should not contain sensitive data
     */
    public static void w(String tag, String message) {
        Log.w(tag, message);
    }
    
    /**
     * Error log - shows in all builds but should not contain sensitive data
     */
    public static void e(String tag, String message) {
        Log.e(tag, message);
    }
    
    /**
     * Error log with exception - shows in all builds but should not contain sensitive data
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
    
    /**
     * Security-focused log for OTP operations - never logs actual values
     */
    public static void securityEvent(String tag, String event) {
        if (BuildConfig.DEBUG_LOGGING) {
            Log.d(tag, "[SECURITY] " + event);
        }
    }
    
    /**
     * Mask sensitive data for logging (phone numbers, OTPs, etc.)
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}
