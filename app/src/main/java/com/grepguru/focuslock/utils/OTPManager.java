package com.grepguru.focuslock.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.security.SecureRandom;
import java.util.Random;

/**
 * OTPManager handles generation, storage, and validation of temporary PINs
 * for accountability partner unlock system.
 */
public class OTPManager {
    private static final String TAG = "OTPManager";
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_CURRENT_OTP = "current_otp";
    private static final String KEY_OTP_EXPIRY = "otp_expiry_time";
    private static final String KEY_OTP_GENERATION_TIME = "otp_generation_time";
    
    // OTP valid for 1 hour (3600000 milliseconds)
    private static final long OTP_VALIDITY_DURATION = 60 * 60 * 1000;
    
    private Context context;
    private SharedPreferences preferences;
    
    public OTPManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Generates a new 4-digit OTP and stores it with expiry time
     * @return Generated OTP as String
     */
    public String generateOTP() {
        // Use SecureRandom for better security
        SecureRandom random = new SecureRandom();
        int otp = 1000 + random.nextInt(9000); // Generates 4-digit number
        
        String otpString = String.valueOf(otp);
        long currentTime = System.currentTimeMillis();
        long expiryTime = currentTime + OTP_VALIDITY_DURATION;
        
        // Store OTP and expiry time
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CURRENT_OTP, otpString);
        editor.putLong(KEY_OTP_EXPIRY, expiryTime);
        editor.putLong(KEY_OTP_GENERATION_TIME, currentTime);
        editor.apply();
        
        Log.d(TAG, "New OTP generated: " + otpString + " (expires in 1 hour)");
        return otpString;
    }
    
    /**
     * Validates if the entered OTP is correct and not expired
     * @param enteredOTP The OTP entered by user
     * @return true if valid, false otherwise
     */
    public boolean validateOTP(String enteredOTP) {
        if (enteredOTP == null || enteredOTP.trim().isEmpty()) {
            return false;
        }
        
        String storedOTP = preferences.getString(KEY_CURRENT_OTP, "");
        long expiryTime = preferences.getLong(KEY_OTP_EXPIRY, 0);
        long currentTime = System.currentTimeMillis();
        
        // Check if OTP exists
        if (storedOTP.isEmpty()) {
            Log.d(TAG, "No OTP found");
            return false;
        }
        
        // Check if OTP is expired
        if (currentTime > expiryTime) {
            Log.d(TAG, "OTP expired");
            clearOTP(); // Clean up expired OTP
            return false;
        }
        
        // Check if OTP matches
        boolean isValid = storedOTP.equals(enteredOTP.trim());
        if (isValid) {
            Log.d(TAG, "OTP validated successfully");
            clearOTP(); // Clear OTP after successful use
        } else {
            Log.d(TAG, "Invalid OTP entered");
        }
        
        return isValid;
    }
    
    /**
     * Sends OTP via SMS to the configured accountability partner with enhanced error handling
     * @param phoneNumber Partner's phone number
     * @param otp The OTP to send
     * @param countryCode Country code for international support
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendOTPviaSMS(String phoneNumber, String otp, String countryCode) {
        // Check SMS permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted");
            Toast.makeText(context, "SMS permission required. Please enable in app settings.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "Partner phone number not configured");
            Toast.makeText(context, "Partner phone number not configured", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            // Format phone number with country code
            String formattedPhone = formatPhoneNumber(phoneNumber, countryCode);
            if (formattedPhone == null) {
                Toast.makeText(context, "Invalid phone number format", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            String message = String.format(
                "🔒 Focus Lock Unlock Code: %s\n\n" +
                "Your friend needs this code to unlock their focus session. " +
                "Code expires in 1 hour.\n\n" +
                "Reply STOP to opt out.\n\n- Focus Lock App", 
                otp
            );
            
            SmsManager smsManager = SmsManager.getDefault();
            
            // Handle long messages by splitting into parts
            if (message.length() > 160) {
                java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(formattedPhone, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(formattedPhone, null, message, null, null);
            }
            
            Log.d(TAG, "OTP SMS sent successfully to: " + maskPhoneNumber(formattedPhone));
            Toast.makeText(context, "OTP sent to accountability partner (" + 
                         maskPhoneNumber(formattedPhone) + ")", Toast.LENGTH_SHORT).show();
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "SMS permission error: " + e.getMessage());
            Toast.makeText(context, "SMS permission required. Please enable in app settings.", Toast.LENGTH_LONG).show();
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid phone number: " + e.getMessage());
            Toast.makeText(context, "Invalid phone number format", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS: " + e.getMessage());
            String errorMsg = "SMS failed: " + (e.getMessage() != null ? e.getMessage() : "Network error");
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            return false;
        }
    }
    
    /**
     * Sends OTP via Email (placeholder for future implementation)
     */
    public boolean sendOTPviaEmail(String email, String otp) {
        Toast.makeText(context, "📧 Email support coming soon! Using SMS instead.", Toast.LENGTH_SHORT).show();
        
        // Fall back to SMS if phone is available
        String partnerPhone = preferences.getString("partner_phone", "");
        String countryCode = preferences.getString("partner_country_code", "");
        
        if (!partnerPhone.isEmpty()) {
            return sendOTPviaSMS(partnerPhone, otp, countryCode);
        }
        
        return false;
    }
    
    /**
     * Validates and formats phone number with country code support
     */
    private String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        
        // Clean phone number (remove spaces, dashes, brackets)
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        
        // If phone starts with +, use as is
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        
        // Add country code if provided
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            String code = countryCode.trim();
            if (!code.startsWith("+")) {
                code = "+" + code;
            }
            return code + cleaned;
        }
        
        // Return cleaned number as is for local numbers
        return cleaned;
    }
    
    /**
     * Masks phone number for display (shows only last 4 digits)
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        
        String visible = phoneNumber.substring(phoneNumber.length() - 4);
        return "****" + visible;
    }
    
    /**
     * Checks if there's a valid (non-expired) OTP
     * @return true if valid OTP exists, false otherwise
     */
    public boolean hasValidOTP() {
        String storedOTP = preferences.getString(KEY_CURRENT_OTP, "");
        long expiryTime = preferences.getLong(KEY_OTP_EXPIRY, 0);
        long currentTime = System.currentTimeMillis();
        
        return !storedOTP.isEmpty() && currentTime <= expiryTime;
    }
    
    /**
     * Gets remaining time for current OTP in milliseconds
     * @return remaining time or 0 if no valid OTP
     */
    public long getRemainingOTPTime() {
        if (!hasValidOTP()) {
            return 0;
        }
        
        long expiryTime = preferences.getLong(KEY_OTP_EXPIRY, 0);
        long currentTime = System.currentTimeMillis();
        
        return Math.max(0, expiryTime - currentTime);
    }
    
    /**
     * Gets formatted remaining time as "MM:SS"
     * @return formatted time string
     */
    public String getFormattedRemainingTime() {
        long remainingMs = getRemainingOTPTime();
        if (remainingMs <= 0) {
            return "Expired";
        }
        
        long minutes = (remainingMs / 1000) / 60;
        long seconds = (remainingMs / 1000) % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Clears stored OTP
     */
    public void clearOTP() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_CURRENT_OTP);
        editor.remove(KEY_OTP_EXPIRY);
        editor.remove(KEY_OTP_GENERATION_TIME);
        editor.apply();
        
        Log.d(TAG, "OTP cleared");
    }
    
    /**
     * Generates OTP and sends it to configured partner based on notification preferences
     * @return true if successful, false otherwise
     */
    public boolean requestOTPFromPartner() {
        // Get notification preferences
        boolean smsEnabled = preferences.getBoolean("enable_sms_notifications", false);
        boolean emailEnabled = preferences.getBoolean("enable_email_notifications", false);
        String partnerPhone = preferences.getString("partner_phone", "");
        String partnerEmail = preferences.getString("partner_email", "");
        String countryCode = preferences.getString("partner_country_code", "");
        
        // Check if any notification method is enabled
        if (!smsEnabled && !emailEnabled) {
            Toast.makeText(context, "⚠️ No notification methods enabled. Configure SMS or Email in settings.", 
                         Toast.LENGTH_LONG).show();
            return false;
        }
        
        // Validate configurations
        if (smsEnabled && partnerPhone.isEmpty()) {
            Toast.makeText(context, "⚠️ SMS enabled but no phone number configured.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (emailEnabled && partnerEmail.isEmpty()) {
            Toast.makeText(context, "⚠️ Email enabled but no email address configured.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        // Generate new OTP every time - user can send as many as they want
        String otp = generateOTP();
        boolean smsSuccess = false;
        boolean emailSuccess = false;
        
        // Send via SMS if enabled
        if (smsEnabled && !partnerPhone.isEmpty()) {
            smsSuccess = sendOTPviaSMS(partnerPhone, otp, countryCode);
        }
        
        // Send via Email if enabled
        if (emailEnabled && !partnerEmail.isEmpty()) {
            emailSuccess = sendOTPviaEmail(partnerEmail, otp);
        }
        
        // Return true if at least one method succeeded
        boolean anySuccess = smsSuccess || emailSuccess;
        
        if (!anySuccess) {
            clearOTP(); // Clear OTP if sending failed
            Toast.makeText(context, "❌ Failed to send unlock code. Check network connection or phone number.", 
                         Toast.LENGTH_LONG).show();
        }
        
        return anySuccess;
    }
}
