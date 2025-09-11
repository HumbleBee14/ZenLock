package com.grepguru.zenlock;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.grepguru.zenlock.utils.OTPManager;
import java.util.regex.Pattern;

public class PartnerContactActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    
    private SwitchCompat smsToggle;
    private LinearLayout smsInputContainer;
    private EditText partnerPhoneInput, countryCodeInput, partnerEmailInput;
    private Button saveButton, testOtpButton;
    private TextView permissionWarning;
    private SharedPreferences preferences;
    private OTPManager otpManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_partner_contact);

        // Handle system bar insets
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        // Initialize components
        preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        otpManager = new OTPManager(this);
        
        smsToggle = findViewById(R.id.smsToggle);
        smsInputContainer = findViewById(R.id.smsInputContainer);
        partnerPhoneInput = findViewById(R.id.partnerPhoneInput);
        countryCodeInput = findViewById(R.id.countryCodeInput);
        partnerEmailInput = findViewById(R.id.partnerEmailInput);
        saveButton = findViewById(R.id.saveButton);
        testOtpButton = findViewById(R.id.testOtpButton);
        permissionWarning = findViewById(R.id.permissionWarning);

        // Load existing settings
        loadSettings();

        // Set up listeners
        setupListeners();
        
        // Check and request SMS permission
        checkSmsPermission();
    }

    private void loadSettings() {
        boolean smsEnabled = preferences.getBoolean("enable_sms_notifications", false);
        String partnerPhone = preferences.getString("partner_phone", "");
        String countryCode = preferences.getString("partner_country_code", "");
        String partnerEmail = preferences.getString("partner_email", "");
        
        smsToggle.setChecked(smsEnabled);
        partnerPhoneInput.setText(partnerPhone);
        countryCodeInput.setText(countryCode);
        partnerEmailInput.setText(partnerEmail);
        
        // Disable email input as it's a future feature
        partnerEmailInput.setEnabled(false);
        partnerEmailInput.setAlpha(0.5f);
        partnerEmailInput.setHint("Email feature coming soon...");
        
        // Set default country code if empty
        if (countryCode.isEmpty()) {
            countryCodeInput.setText("+1"); // Default to US
        }
        
        // Show/hide phone input based on toggle
        smsInputContainer.setVisibility(smsEnabled ? View.VISIBLE : View.GONE);
        
        // Update test button state based on phone input
        updateTestButtonState();
        
        // Update save button state based on permissions
        updateSaveButtonState();
    }

    private void setupListeners() {
        // SMS Toggle Listener
        smsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            smsInputContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            if (isChecked) {
                checkSmsPermission();
            }
            
            // Always update both button states when toggle changes
            updateTestButtonState();
            updateSaveButtonState();
        });

        // Phone input text watcher to enable/disable test button
        partnerPhoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTestButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save Button Listener
        saveButton.setOnClickListener(v -> saveConfiguration());
        
        // Test OTP Button Listener
        testOtpButton.setOnClickListener(v -> {
            // Always check permission first, if not granted, request it
            if (!checkSmsPermission()) {
                requestSmsPermission();
            } else {
                // Permission is granted, proceed with normal OTP functionality
                testOTPFunctionality();
            }
        });
    }
    
    private void updateTestButtonState() {
        boolean smsEnabled = smsToggle.isChecked();
        String phoneText = partnerPhoneInput.getText().toString().trim();
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        
        if (smsEnabled && !phoneText.isEmpty() && hasSmsPermission) {
            testOtpButton.setEnabled(true);
            testOtpButton.setAlpha(1.0f);
            testOtpButton.setText("Send Test OTP");
            testOtpButton.setVisibility(View.VISIBLE);
        } else if (smsEnabled && !hasSmsPermission) {
            // Keep button enabled but with different text for permission request
            testOtpButton.setEnabled(true);
            testOtpButton.setAlpha(0.7f);
            testOtpButton.setText("Grant SMS Permission");
            testOtpButton.setVisibility(View.VISIBLE);
        } else {
            testOtpButton.setEnabled(false);
            testOtpButton.setAlpha(0.5f);
            testOtpButton.setText("Send Test OTP");
            testOtpButton.setVisibility(smsEnabled ? View.VISIBLE : View.GONE);
        }
        
        // Also update save button state
        updateSaveButtonState();
    }
    
    private void updateSaveButtonState() {
        boolean smsEnabled = smsToggle.isChecked();
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        
        if (smsEnabled && !hasSmsPermission) {
            // SMS enabled but no permission - disable save button
            saveButton.setEnabled(false);
            saveButton.setAlpha(0.5f);
        } else {
            // Either SMS disabled or SMS enabled with permission - enable save button
            saveButton.setEnabled(true);
            saveButton.setAlpha(1.0f);
        }
    }

    private void saveConfiguration() {
        boolean smsEnabled = smsToggle.isChecked();
        String partnerPhone = partnerPhoneInput.getText().toString().trim();
        String countryCode = countryCodeInput.getText().toString().trim();
        String partnerEmail = partnerEmailInput.getText().toString().trim();

        // First check SMS permission if SMS is enabled
        if (smsEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Cannot save SMS configuration without SMS permission. Please grant permission first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate SMS configuration
        if (smsEnabled && partnerPhone.isEmpty()) {
            Toast.makeText(this, "Please enter partner's phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (smsEnabled && !isValidPhoneNumber(partnerPhone)) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (smsEnabled && !isValidCountryCode(countryCode)) {
            Toast.makeText(this, "Please enter a valid country code (e.g., +1, +44, +91)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email if provided
        if (!partnerEmail.isEmpty() && !isValidEmail(partnerEmail)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("enable_sms_notifications", smsEnabled);
        editor.putString("partner_phone", partnerPhone);
        editor.putString("partner_country_code", countryCode);
        editor.putString("partner_email", partnerEmail);
        editor.apply();

        // Show success message
        String message = smsEnabled ? 
            "SMS partner contact configured successfully!" : 
            "Partner contact disabled successfully!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Update test button state after save
        updateTestButtonState();

        // Close activity
        finish();
    }
    
    private void testOTPFunctionality() {
        if (!checkSmsPermission()) {
            return;
        }
        
        String partnerPhone = partnerPhoneInput.getText().toString().trim();
        String countryCode = countryCodeInput.getText().toString().trim();
        
        if (partnerPhone.isEmpty()) {
            Toast.makeText(this, "Please enter partner phone number first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isValidPhoneNumber(partnerPhone)) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isValidCountryCode(countryCode)) {
            Toast.makeText(this, "Please enter a valid country code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Generate and send test OTP
        String testOtp = otpManager.generateOTP();
        boolean sent = otpManager.sendOTPviaSMS(partnerPhone, testOtp, countryCode);
        
        if (sent) {
            Toast.makeText(this, "Test OTP sent successfully! Check with your partner.", Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            
            // Show permission warning
            if (permissionWarning != null) {
                permissionWarning.setVisibility(View.VISIBLE);
                permissionWarning.setText("⚠️ SMS permission required to send OTP. Click here to grant permission.");
                permissionWarning.setOnClickListener(v -> requestSmsPermission());
            }
            
            return false;
        } else {
            // Hide permission warning
            if (permissionWarning != null) {
                permissionWarning.setVisibility(View.GONE);
            }
            return true;
        }
    }
    
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.SEND_SMS}, 
            SMS_PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show();
                checkSmsPermission(); // Update UI
                updateTestButtonState(); // Update button states
                updateSaveButtonState(); // Update save button state
            } else {
                Toast.makeText(this, "SMS permission denied. OTP functionality will not work.", Toast.LENGTH_LONG).show();
                updateTestButtonState(); // Update button states even if denied
                updateSaveButtonState(); // Update save button state
            }
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        // Enhanced phone number validation
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return cleanPhone.length() >= 7 && cleanPhone.length() <= 15 && cleanPhone.matches("\\d+");
    }
    
    private boolean isValidCountryCode(String countryCode) {
        // Validate country code format
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return false;
        }
        
        String cleaned = countryCode.trim();
        // Must start with + and have 1-4 digits
        Pattern countryCodePattern = Pattern.compile("^\\+[1-9]\\d{0,3}$");
        return countryCodePattern.matcher(cleaned).matches();
    }
    
    private boolean isValidEmail(String email) {
        // Basic email validation
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        return emailPattern.matcher(email.trim()).matches();
    }
}