package com.grepguru.focuslock.utils;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class OTPManager {

    private static final String TAG = "OTPManager";
    private static final String PREF_NAME = "OTPManagerPrefs";
    private static final String KEY_CURRENT_OTP = "current_otp";
    private static final String KEY_OTP_EXPIRY = "otp_expiry";
    private static final String KEY_OTP_GENERATION_TIME = "otp_generation_time";
    private static final long OTP_VALIDITY_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    // Actions for BroadcastReceivers
    private static final String ACTION_SMS_SENT = "com.grepguru.focuslock.SMS_SENT_ACTION";
    private static final String ACTION_SMS_DELIVERED = "com.grepguru.focuslock.SMS_DELIVERED_ACTION";

    private final Context context;
    private final SharedPreferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final AtomicInteger pendingIntentRequestCode = new AtomicInteger(0);


    public OTPManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid activity leaks
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Generates a 4-digit OTP, stores it with an expiry time.
     * @return The generated OTP.
     */
    public String generateOTP() {
        int otpValue = 1000 + secureRandom.nextInt(9000); // 4-digit OTP (1000-9999)
        String otp = String.format(Locale.US, "%04d", otpValue);

        long generationTime = System.currentTimeMillis();
        long expiryTime = generationTime + OTP_VALIDITY_DURATION_MS;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CURRENT_OTP, otp);
        editor.putLong(KEY_OTP_EXPIRY, expiryTime);
        editor.putLong(KEY_OTP_GENERATION_TIME, generationTime);
        editor.apply();

        Log.d(TAG, "Generated OTP: " + otp + " (Expires at: " + expiryTime + ")");
        return otp;
    }

    /**
     * Verifies the provided OTP against the stored OTP.
     * @param otpToVerify The OTP string to verify.
     * @return true if the OTP is valid and not expired, false otherwise.
     */
    public boolean verifyOTP(String otpToVerify) {
        String storedOTP = preferences.getString(KEY_CURRENT_OTP, null);
        long expiryTime = preferences.getLong(KEY_OTP_EXPIRY, 0);

        if (storedOTP == null || otpToVerify == null) {
            Log.w(TAG, "Verification failed: Stored or provided OTP is null.");
            return false;
        }

        if (System.currentTimeMillis() > expiryTime) {
            Log.w(TAG, "Verification failed: OTP expired. Current time: " + System.currentTimeMillis() + ", Expiry time: " + expiryTime);
            clearOTP(); // Clear expired OTP
            return false;
        }

        boolean isValid = storedOTP.equals(otpToVerify);
        if (isValid) {
            Log.d(TAG, "OTP verified successfully: " + otpToVerify);
        } else {
            Log.w(TAG, "Verification failed: OTP mismatch. Expected: " + storedOTP + ", Got: " + otpToVerify);
        }
        return isValid;
    }

    /**
     * Sends the OTP via SMS to the specified phone number.
     * Assumes SEND_SMS permission has been granted.
     * @param phoneNumber The phone number to send the SMS to.
     * @param otp The OTP to send.
     * @param countryCode The country code for the phone number.
     * @return true if the SMS send process was initiated, false otherwise.
     */
    public boolean sendOTPviaSMS(String phoneNumber, String otp, String countryCode) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted.");
            Toast.makeText(context, "❌ SEND_SMS permission is required to send OTP.", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!isDeviceSmsCapable()) {
            Log.e(TAG, "Device is not capable of sending SMS.");
            Toast.makeText(context, "❌ Device cannot send SMS. Check SIM status or network.", Toast.LENGTH_LONG).show();
            return false;
        }

        String formattedPhone = formatPhoneNumber(phoneNumber, countryCode);
        if (formattedPhone == null || formattedPhone.trim().isEmpty()) {
            Log.e(TAG, "Invalid phone number provided for SMS.");
            Toast.makeText(context, "❌ Invalid phone number for SMS.", Toast.LENGTH_LONG).show();
            return false;
        }

        String message = "Your FocusLock OTP is: " + otp;

        try {
            // Since minSdk is 28 (which is >= Build.VERSION_CODES.M (23)),
            // we can directly use getSystemService(SmsManager.class)
            SmsManager smsManager = context.getSystemService(SmsManager.class);

            if (smsManager == null) {
                Log.e(TAG, "SmsManager not available.");
                Toast.makeText(context, "❌ SMS service not available on this device.", Toast.LENGTH_LONG).show();
                return false;
            }

            Intent sentIntent = new Intent(ACTION_SMS_SENT);
            sentIntent.setPackage(context.getPackageName()); // Explicitly target this app's receiver
            PendingIntent sentPI = PendingIntent.getBroadcast(
                    context,
                    pendingIntentRequestCode.getAndIncrement(),
                    sentIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            Intent deliveredIntent = new Intent(ACTION_SMS_DELIVERED);
            deliveredIntent.setPackage(context.getPackageName()); // Explicitly target this app's receiver
            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                    context,
                    pendingIntentRequestCode.getAndIncrement(),
                    deliveredIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );
            
            // Log debugging information
            try {
                logSmsDebuggingInfo(formattedPhone);
            } catch (Exception debugError) {
                Log.w(TAG, "Debug logging failed, but continuing with SMS: " + debugError.getMessage());
            }

            if (message.length() > 160) { // Max length for single SMS
                ArrayList<String> parts = smsManager.divideMessage(message);
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveryIntents = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    sentIntents.add(sentPI);
                    deliveryIntents.add(deliveredPI);
                }
                smsManager.sendMultipartTextMessage(formattedPhone, null, parts, sentIntents, deliveryIntents);
            } else {
                smsManager.sendTextMessage(formattedPhone, null, message, sentPI, deliveredPI);
            }

            Log.d(TAG, "SMS send attempt initiated to: " + maskPhoneNumber(formattedPhone));
            Toast.makeText(context, "📤 Sending OTP to partner...", Toast.LENGTH_SHORT).show();
            return true;

        } catch (SecurityException e) {
            Log.e(TAG, "SMS permission error during sending: " + e.getMessage(), e);
            Toast.makeText(context, "❌ SMS permission denied. Check app settings.", Toast.LENGTH_LONG).show();
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid argument for SMS: " + e.getMessage(), e);
            Toast.makeText(context, "❌ Invalid argument for SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS: " + e.getMessage(), e);
            String errorMsg = "❌ SMS failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Placeholder for sending OTP via Email.
     * @param otp The OTP to send.
     * @return false, as email is not yet implemented.
     */
    public boolean sendOTPviaEmail(String otp) { // Removed unused 'email' parameter
        Toast.makeText(context, "📧 Email support coming soon! Using SMS if available.", Toast.LENGTH_LONG).show();
        
        // Fallback to SMS if partner phone is available (as per original logic)
        String partnerPhone = preferences.getString("partner_phone", "");
        String countryCode = preferences.getString("partner_country_code", "");
        
        if (!partnerPhone.isEmpty()) {
            Log.d(TAG, "Falling back to SMS for OTP delivery as email is not implemented.");
            return sendOTPviaSMS(partnerPhone, otp, countryCode);
        }
        Log.w(TAG, "Email not implemented and no partner phone for SMS fallback.");
        return false;
    }

    private String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "Phone number is null or empty");
            return null;
        }
        
        boolean startsWithPlus = phoneNumber.trim().startsWith("+");
        // Removes spaces, hyphens, parentheses, dots. Keeps plus if explicitly at start.
        String cleaned = phoneNumber.replaceAll("[\\s\\-().]", ""); 
        if (startsWithPlus && !cleaned.startsWith("+")) { // Ensure plus is preserved if originally there
            cleaned = "+" + cleaned.replaceAll("\\+", ""); // Remove other pluses
        } else {
            cleaned = cleaned.replaceAll("\\+", ""); // Remove all pluses if not explicitly international
        }


        if (!cleaned.matches("^\\+?\\d+$") && !cleaned.matches("^\\d+$")) { // Allows optional plus at start, then digits
             Log.e(TAG, "Phone number contains invalid characters after cleaning: " + cleaned + " (Original: " + phoneNumber + ")");
             return null;
        }
        
        // Remove leading plus for length checks if it was not part of original input for country code logic
        String checkableNumber = cleaned.startsWith("+") ? cleaned.substring(1) : cleaned;

        if (checkableNumber.length() < 7 && !startsWithPlus) { // Stricter for local, bit more lenient for full intl numbers
            Log.e(TAG, "Phone number too short: " + checkableNumber.length() + " digits. Original: " + phoneNumber);
            return null;
        }
        if (checkableNumber.length() > 15) {
            Log.e(TAG, "Phone number too long: " + checkableNumber.length() + " digits. Original: " + phoneNumber);
            return null;
        }

        if (startsWithPlus) { // Already an international number
            if(!cleaned.startsWith("+")) cleaned = "+" + cleaned; // Should be redundant due to earlier logic but safe
            Log.d(TAG, "Using provided international number: " + maskPhoneNumber(cleaned));
            return cleaned;
        }

        if (countryCode != null && !countryCode.trim().isEmpty()) {
            String code = countryCode.trim().replaceAll("[^\\d]", "");
            if (code.isEmpty() || code.length() > 4) {
                Log.e(TAG, "Invalid country code format: " + countryCode);
                 // Proceed with cleaned number as local if country code is bad
            } else {
                 String fullNumber = "+" + code + checkableNumber;
                 Log.d(TAG, "Formatted international number: " + maskPhoneNumber(fullNumber));
                 return fullNumber;
            }
        }
        
        Log.d(TAG, "Using local number format: " + maskPhoneNumber(cleaned)); // `cleaned` might still have a + if entered directly
        return cleaned; // Assuming local number or already internationalized by user if no valid country code
    }
    
    private void logSmsDebuggingInfo(String formattedPhoneNumber) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted. SMS debugging info will be limited.");
            // Optionally, inform the user or request permission via the calling Activity.
            // For now, just log and continue.
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            Log.e(TAG, "TelephonyManager not available for debugging.");
            return;
        }

        Log.i(TAG, "--- SMS Debugging Info ---");
        Log.i(TAG, "Target Phone (Formatted): " + maskPhoneNumber(formattedPhoneNumber));
        Log.i(TAG, "Device SMS Capable (as per isDeviceSmsCapable()): " + isDeviceSmsCapable());
        Log.i(TAG, "SIM State: " + getSimStateString(tm.getSimState()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.i(TAG, "Default SMS Subscription ID: " + SmsManager.getDefaultSmsSubscriptionId());
            // The following line was causing a compile error and has been commented out.
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //    try {
            //        Log.i(TAG, "Default Data Subscription ID: " + android.telephony.SubscriptionManager.getDefaultDataSubscriptionId());
            //    } catch (Exception e) {
            //        Log.w(TAG, "Could not get default data subscription ID: " + e.getMessage());
            //    }
            // }
        }
        Log.i(TAG, "Network Operator: " + tm.getNetworkOperatorName());
        Log.i(TAG, "Network Country ISO: " + tm.getNetworkCountryIso());
        Log.i(TAG, "--- End SMS Debugging Info ---");
    }


    /**
     * Requests READ_PHONE_STATE permission if not already granted.
     * This is optional for SMS functionality but helps with debugging.
     * Call this from an Activity context.
     */
    public void requestPhoneStatePermissionIfNeeded(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Requesting READ_PHONE_STATE permission for enhanced SMS debugging.");
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    1001); // Request code for phone state permission
        }
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****"; // Or return original if too short to mask meaningfully
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    public boolean hasValidOTP() {
        String storedOTP = preferences.getString(KEY_CURRENT_OTP, null);
        long expiryTime = preferences.getLong(KEY_OTP_EXPIRY, 0);
        return storedOTP != null && !storedOTP.isEmpty() && System.currentTimeMillis() <= expiryTime;
    }

    public long getRemainingOTPTime() {
        if (!hasValidOTP()) {
            return 0;
        }
        long expiryTime = preferences.getLong(KEY_OTP_EXPIRY, 0);
        return Math.max(0, expiryTime - System.currentTimeMillis());
    }

    public void clearOTP() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_CURRENT_OTP);
        editor.remove(KEY_OTP_EXPIRY);
        editor.remove(KEY_OTP_GENERATION_TIME);
        editor.apply();
        Log.d(TAG, "OTP cleared from SharedPreferences.");
    }

    /**
     * Attempts to request an OTP and send it via enabled notification channels.
     * @return true if OTP sending was successfully initiated through at least one channel, false otherwise.
     */
    public boolean requestOTPFromPartner() {
        boolean smsEnabled = preferences.getBoolean("enable_sms_notifications", false);
        // boolean emailEnabled = preferences.getBoolean("enable_email_notifications", false); // Kept for future use

        String partnerPhone = preferences.getString("partner_phone", "");
        // String partnerEmail = preferences.getString("partner_email", ""); // Kept for future use
        String countryCode = preferences.getString("partner_country_code", "");

        if (!smsEnabled) { // Currently, only SMS is fully implemented
            Toast.makeText(context, "⚠️ SMS notifications not enabled. Configure in settings.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "OTP request failed: SMS notifications not enabled.");
            return false;
        }

        String otp = generateOTP();
        boolean anySuccess = false;

        if (smsEnabled) {
            if (partnerPhone.isEmpty()) {
                Toast.makeText(context, "⚠️ SMS enabled but no partner phone number configured.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "OTP request failed: Partner phone number missing for SMS.");
            } else {
                anySuccess = sendOTPviaSMS(partnerPhone, otp, countryCode);
            }
        }

        // Future: Add email logic here if emailEnabled and !anySuccess
        // if (emailEnabled && !anySuccess) { ... }

        if (!anySuccess) {
            clearOTP(); // Clear OTP if all sending attempts failed
            Toast.makeText(context, "❌ Failed to send unlock code. Check settings and network.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "OTP request failed: All sending methods failed or were not configured.");
        } else {
            Log.d(TAG, "OTP request processed. Success status: " + anySuccess);
        }
        return anySuccess;
    }

    private boolean isDeviceSmsCapable() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot accurately determine SIM state without READ_PHONE_STATE. Assuming SMS capable if Telephony feature exists.");
            // If READ_PHONE_STATE is not granted, we can only check for the telephony feature.
            // This is a less reliable check for full capability.
             return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
                   (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING));
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            Log.e(TAG, "TelephonyManager not available. Assuming SMS not capable.");
            return false;
        }
        
        boolean hasTelephonyFeature = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
                                      (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING));

        if (!hasTelephonyFeature) {
            Log.e(TAG, "Device does not have telephony or messaging feature.");
            return false;
        }
        
        boolean simReady = tm.getSimState() == TelephonyManager.SIM_STATE_READY;
        Log.d(TAG, "Device SMS capability - Has Telephony/Messaging: " + hasTelephonyFeature + ", SIM Ready: " + simReady + ", SIM State: " + getSimStateString(tm.getSimState()));
        
        return hasTelephonyFeature && simReady;
    }
    
    private String getSimStateString(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT: return "No SIM card is available";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "SIM card is network locked";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "SIM card requires PIN";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "SIM card requires PUK";
            case TelephonyManager.SIM_STATE_READY: return "SIM card is ready";
            case TelephonyManager.SIM_STATE_NOT_READY: return "SIM card is not ready";
            case TelephonyManager.SIM_STATE_PERM_DISABLED: return "SIM card is permanently disabled";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR: return "SIM card error (IO Error)";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED: return "SIM card restricted";
            default: return "Unknown SIM state (" + simState + ")";
        }
    }

    // --- BroadcastReceivers for SMS Sent/Delivered Callbacks ---

    public static class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_SMS_SENT.equals(action)) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.d(TAG, "SmsSentReceiver: SMS sent successfully.");
                        Toast.makeText(context, "✅ OTP SMS sent.", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.e(TAG, "SmsSentReceiver: Generic failure.");
                        Toast.makeText(context, "❌ SMS send failed (Generic).", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.e(TAG, "SmsSentReceiver: No service.");
                        Toast.makeText(context, "❌ SMS send failed (No service).", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.e(TAG, "SmsSentReceiver: Null PDU.");
                        Toast.makeText(context, "❌ SMS send failed (Null PDU).", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.e(TAG, "SmsSentReceiver: Radio off.");
                        Toast.makeText(context, "❌ SMS send failed (Radio off).", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Log.e(TAG, "SmsSentReceiver: Unknown error or non-standard result code: " + getResultCode());
                        Toast.makeText(context, "❌ SMS send failed (Unknown error code: " + getResultCode() + ").", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    }

    public static class SmsDeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
            if (ACTION_SMS_DELIVERED.equals(action)) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.d(TAG, "SmsDeliveredReceiver: SMS delivered successfully.");
                        Toast.makeText(context, "✅ OTP SMS delivered.", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED: // Or other non-OK codes for failure
                        Log.e(TAG, "SmsDeliveredReceiver: SMS not delivered. Result code: " + getResultCode());
                        Toast.makeText(context, "❌ SMS not delivered.", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Log.w(TAG, "SmsDeliveredReceiver: SMS delivery status code: " + getResultCode());
                        // Avoid showing a toast for every possible status code here unless it's critical
                        break;
                }
            }
        }
    }
}
