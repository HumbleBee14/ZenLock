package com.grepguru.focuslock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class PartnerContactActivity extends AppCompatActivity {

    private SwitchCompat smsToggle;
    private LinearLayout smsInputContainer;
    private EditText partnerPhoneInput;
    private Button saveButton;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_contact);

        // Initialize components
        preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        
        smsToggle = findViewById(R.id.smsToggle);
        smsInputContainer = findViewById(R.id.smsInputContainer);
        partnerPhoneInput = findViewById(R.id.partnerPhoneInput);
        saveButton = findViewById(R.id.saveButton);

        // Load existing settings
        loadSettings();

        // Set up listeners
        setupListeners();
    }

    private void loadSettings() {
        boolean smsEnabled = preferences.getBoolean("partner_sms_enabled", false);
        String partnerPhone = preferences.getString("partner_phone", "");
        
        smsToggle.setChecked(smsEnabled);
        partnerPhoneInput.setText(partnerPhone);
        
        // Show/hide phone input based on toggle
        smsInputContainer.setVisibility(smsEnabled ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        // SMS Toggle Listener
        smsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            smsInputContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            if (!isChecked) {
                // Clear phone number when SMS is disabled
                partnerPhoneInput.setText("");
            }
        });

        // Save Button Listener
        saveButton.setOnClickListener(v -> saveConfiguration());
    }

    private void saveConfiguration() {
        boolean smsEnabled = smsToggle.isChecked();
        String partnerPhone = partnerPhoneInput.getText().toString().trim();

        // Validate SMS configuration
        if (smsEnabled && partnerPhone.isEmpty()) {
            Toast.makeText(this, "Please enter partner's phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (smsEnabled && !isValidPhoneNumber(partnerPhone)) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("partner_sms_enabled", smsEnabled);
        editor.putString("partner_phone", partnerPhone);
        editor.apply();

        // Show success message
        String message = smsEnabled ? 
            "SMS partner contact configured successfully!" : 
            "Partner contact disabled successfully!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Close activity
        finish();
    }

    private boolean isValidPhoneNumber(String phone) {
        // Basic phone number validation (can be enhanced)
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return cleanPhone.length() >= 10 && cleanPhone.matches("\\d+");
    }
} 