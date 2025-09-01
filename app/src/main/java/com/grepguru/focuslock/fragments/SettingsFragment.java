package com.grepguru.focuslock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.grepguru.focuslock.R;
import com.grepguru.focuslock.model.UnlockMethod;
import com.grepguru.focuslock.WhitelistActivity;

public class SettingsFragment extends Fragment {

    private SwitchCompat superStrictModeToggle, defaultAppsToggle, quotesToggle;
    private SwitchCompat persistentNotificationToggle, autoRestartToggle;
    private RadioGroup securityLevelGroup;
    private RadioButton basicSecurity, enhancedSecurity, maximumSecurity;
    
    // Expandable UI elements
    private LinearLayout lockProtectionHeader, lockProtectionContent;
    private ImageView lockProtectionExpandIcon;
    private TextView lockProtectionSummary;
    private SharedPreferences preferences;
    
    // Enhanced unlock UI elements
    private Button configurePinButton, clearPinButton;
    private Button accountabilityPartnerButton;
    private SwitchCompat pinUnlockToggle, partnerUnlockToggle;
    private LinearLayout pinConfigSection, partnerConfigSection;
    private LinearLayout noUnlockMethodsWarning;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize components
        preferences = requireActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);

        superStrictModeToggle = view.findViewById(R.id.superStrictModeToggle);
        defaultAppsToggle = view.findViewById(R.id.defaultAppsToggle);
        quotesToggle = view.findViewById(R.id.quotesToggle);
        
        // Security settings
        persistentNotificationToggle = view.findViewById(R.id.persistentNotificationToggle);
        autoRestartToggle = view.findViewById(R.id.autoRestartToggle);
        securityLevelGroup = view.findViewById(R.id.securityLevelGroup);
        basicSecurity = view.findViewById(R.id.basicSecurity);
        enhancedSecurity = view.findViewById(R.id.enhancedSecurity);
        maximumSecurity = view.findViewById(R.id.maximumSecurity);
        
        // Expandable UI elements
        lockProtectionHeader = view.findViewById(R.id.lockProtectionHeader);
        lockProtectionContent = view.findViewById(R.id.lockProtectionContent);
        lockProtectionExpandIcon = view.findViewById(R.id.lockProtectionExpandIcon);
        lockProtectionSummary = view.findViewById(R.id.lockProtectionSummary);

        // Enhanced unlock UI elements
        configurePinButton = view.findViewById(R.id.configurePinButton);
        clearPinButton = view.findViewById(R.id.clearPinButton);
        accountabilityPartnerButton = view.findViewById(R.id.accountabilityPartnerButton);
        
        // Toggle switches for unlock methods
        pinUnlockToggle = view.findViewById(R.id.pinUnlockToggle);
        partnerUnlockToggle = view.findViewById(R.id.partnerUnlockToggle);
        
        // Expandable sections
        pinConfigSection = view.findViewById(R.id.pinConfigSection);
        partnerConfigSection = view.findViewById(R.id.partnerConfigSection);
        
        // Warning message
        noUnlockMethodsWarning = view.findViewById(R.id.noUnlockMethodsWarning);

        // Load existing settings
        superStrictModeToggle.setChecked(preferences.getBoolean("super_strict_mode", false));
        defaultAppsToggle.setChecked(preferences.getBoolean("allow_default_apps", true));
        quotesToggle.setChecked(preferences.getBoolean("show_quotes", true));
        
        // Load security settings
        persistentNotificationToggle.setChecked(preferences.getBoolean("persistent_notification", true));
        autoRestartToggle.setChecked(preferences.getBoolean("auto_restart", true));
        
        // Load security level
        int securityLevel = preferences.getInt("security_level", 1); // Default to enhanced
        switch (securityLevel) {
            case 0:
                basicSecurity.setChecked(true);
                lockProtectionSummary.setText("Basic Security");
                break;
            case 1:
                enhancedSecurity.setChecked(true);
                lockProtectionSummary.setText("Enhanced Security");
                break;
            case 2:
                maximumSecurity.setChecked(true);
                lockProtectionSummary.setText("Maximum Security");
                break;
        }

        // Update PIN button states
        updateUnlockMethodStates();

        // Set up event listeners
        setupListeners(view);

        return view;
    }

    private void setupListeners(View view) {
        Button whitelistButton = view.findViewById(R.id.whitelistButton);

        // Toggle Super Strict Mode
        superStrictModeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("super_strict_mode", isChecked);
            editor.apply();
        });

        // Toggle Default Apps Allowed
        defaultAppsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("allow_default_apps", isChecked);
            editor.apply();
        });

        // Toggle Motivational Quotes
        quotesToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("show_quotes", isChecked);
            editor.apply();
        });
        
        // Security settings listeners
        persistentNotificationToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("persistent_notification", isChecked);
            editor.apply();
        });
        
        autoRestartToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("auto_restart", isChecked);
            editor.apply();
        });
        
        // Security level selection
        securityLevelGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int securityLevel = 1; // Default to enhanced
            if (checkedId == R.id.basicSecurity) {
                securityLevel = 0;
                lockProtectionSummary.setText("Basic Security");
            } else if (checkedId == R.id.enhancedSecurity) {
                securityLevel = 1;
                lockProtectionSummary.setText("Enhanced Security");
            } else if (checkedId == R.id.maximumSecurity) {
                securityLevel = 2;
                lockProtectionSummary.setText("Maximum Security");
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("security_level", securityLevel);
            editor.apply();
        });
        
        // Setup expandable functionality
        setupExpandableSections();

        // Navigate to Whitelist Management
        whitelistButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), WhitelistActivity.class);
            startActivity(intent);
        });

        // PIN Unlock Toggle
        pinUnlockToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                pinConfigSection.setVisibility(View.VISIBLE);
                // Don't auto-prompt for PIN setup - let user click the button
            } else {
                pinConfigSection.setVisibility(View.GONE);
                // Clear PIN when disabled
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("unlock_pin");
                editor.apply();
                updateUnlockMethodStates();
            }
        });

        // Partner Unlock Toggle
        partnerUnlockToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                partnerConfigSection.setVisibility(View.VISIBLE);
                // Don't auto-open configuration - let user click the button
            } else {
                partnerConfigSection.setVisibility(View.GONE);
                // Clear partner settings when disabled
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("partner_phone");
                editor.remove("partner_email");
                editor.remove("enable_sms_notifications");
                editor.remove("enable_email_notifications");
                editor.apply();
                updateUnlockMethodStates();
            }
        });

        // PIN Configuration Listeners
        configurePinButton.setOnClickListener(v -> showPinSetupDialog());
        
        clearPinButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("unlock_pin");
            editor.apply();
            updateUnlockMethodStates();
            Toast.makeText(requireContext(), "PIN cleared", Toast.LENGTH_SHORT).show();
        });

        // Partner Contact Configuration
        accountabilityPartnerButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.grepguru.focuslock.PartnerContactActivity.class);
            startActivity(intent);
        });
    }

    private void updateUnlockMethodStates() {
        // Check PIN status
        String existingPin = preferences.getString("unlock_pin", "");
        boolean pinConfigured = !existingPin.isEmpty();
        
        pinUnlockToggle.setChecked(pinConfigured);
        pinConfigSection.setVisibility(pinConfigured ? View.VISIBLE : View.GONE);
        
        if (pinConfigured) {
            configurePinButton.setText("Change PIN");
            clearPinButton.setVisibility(View.VISIBLE);
        } else {
            configurePinButton.setText("Set PIN");
            clearPinButton.setVisibility(View.GONE);
        }
        
        // Check Partner status
        String partnerPhone = preferences.getString("partner_phone", "");
        boolean partnerConfigured = !partnerPhone.isEmpty();
        
        partnerUnlockToggle.setChecked(partnerConfigured);
        partnerConfigSection.setVisibility(partnerConfigured ? View.VISIBLE : View.GONE);
        
        if (partnerConfigured) {
            accountabilityPartnerButton.setText("Update Contact");
        } else {
            accountabilityPartnerButton.setText("Configure Contact");
        }
        
        // Show/hide warning message based on unlock method availability
        boolean hasAnyUnlockMethod = pinConfigured || partnerConfigured;
        noUnlockMethodsWarning.setVisibility(hasAnyUnlockMethod ? View.GONE : View.VISIBLE);
    }

    private void setupExpandableSections() {
        // Lock Protection expandable section
        lockProtectionHeader.setOnClickListener(v -> {
            boolean isExpanded = lockProtectionContent.getVisibility() == View.VISIBLE;
            
            if (isExpanded) {
                // Collapse
                lockProtectionContent.setVisibility(View.GONE);
                lockProtectionExpandIcon.animate()
                    .rotation(0)
                    .setDuration(200)
                    .start();
            } else {
                // Expand
                lockProtectionContent.setVisibility(View.VISIBLE);
                lockProtectionExpandIcon.animate()
                    .rotation(180)
                    .setDuration(200)
                    .start();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUnlockMethodStates();
    }
    
    private void showPinSetupDialog() {
        String existingPin = preferences.getString("unlock_pin", "");
        if (!existingPin.isEmpty()) {
            // PIN already exists, verify current PIN first
            showCurrentPinVerificationDialog();
        } else {
            // No PIN exists, go directly to setup
            showActualPinDialog();
        }
    }
    
    private void showCurrentPinVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pin_input, null);
        
        EditText pinInput = dialogView.findViewById(R.id.pinInput);
        TextView otpStatusText = dialogView.findViewById(R.id.otpStatusText);
        TextView titleText = dialogView.findViewById(R.id.unlockMethodTitle);
        TextView instructionText = dialogView.findViewById(R.id.unlockMethodDescription);
        
        // Set up for current PIN verification
        titleText.setText("Verify Current PIN");
        instructionText.setText("Enter your current PIN to change it");
        
        // Hide OTP-specific buttons
        Button requestOtpButton = dialogView.findViewById(R.id.requestOtpButton);
        Button sendOtpAgainButton = dialogView.findViewById(R.id.sendOtpAgainButton);
        if (requestOtpButton != null) requestOtpButton.setVisibility(View.GONE);
        if (sendOtpAgainButton != null) sendOtpAgainButton.setVisibility(View.GONE);
        
        // Setup input watcher for visual feedback
        setupCurrentPinInputWatcher(pinInput, otpStatusText);
        
        builder.setView(dialogView)
               .setTitle("PIN Verification")
               .setPositiveButton("Verify", null)
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String enteredPin = pinInput.getText().toString().trim();
                if (validateCurrentPin(enteredPin, pinInput, otpStatusText)) {
                    dialog.dismiss();
                    showActualPinDialog(); // Show PIN setup dialog after successful verification
                }
            });
        });
        
        dialog.show();
    }
    
    private void showActualPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trusted_pin_setup, null);
        
        EditText newPinInput = dialogView.findViewById(R.id.newPinInput);
        EditText confirmPinInput = dialogView.findViewById(R.id.confirmPinInput);
        TextView instructionText = dialogView.findViewById(R.id.instructionText);
        TextView pinStatusText = dialogView.findViewById(R.id.pinStatusText);
        
        // Setup input watchers for visual feedback
        setupPinSetupInputWatcher(newPinInput, confirmPinInput, pinStatusText);
        
        // Check if PIN already exists
        String existingPin = preferences.getString("unlock_pin", "");
        if (!existingPin.isEmpty()) {
            instructionText.setText("⚠️ PIN already configured.\n\nTo change it, enter a new PIN twice below.");
        } else {
            instructionText.setText("💡 Set your unlock PIN here.\n\n" +
                                  "💭 Suggestion: You could ask a friend to set this PIN " +
                                  "for better accountability - that way you won't know what it is!");
        }
        
        builder.setView(dialogView)
               .setTitle("PIN Setup")
               .setPositiveButton("Set PIN", null) // Set to null initially
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        
        // Override positive button to validate before closing
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String newPin = newPinInput.getText().toString().trim();
                String confirmPin = confirmPinInput.getText().toString().trim();
                
                if (validatePinSetup(newPin, confirmPin, newPinInput, confirmPinInput, pinStatusText)) {
                    // Save PIN
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("unlock_pin", newPin);
                    editor.apply();
                    
                    updateUnlockMethodStates();
                    showPinSetupSuccess(pinStatusText);
                    
                    // Close dialog after a brief delay to show success message
                    pinStatusText.postDelayed(() -> dialog.dismiss(), 1500);
                }
            });
        });
        
        dialog.show();
    }
    
    private void setupPinSetupInputWatcher(EditText newPinInput, EditText confirmPinInput, TextView statusText) {
        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetPinSetupInputState(newPinInput, confirmPinInput, statusText);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        };
        
        newPinInput.addTextChangedListener(inputWatcher);
        confirmPinInput.addTextChangedListener(inputWatcher);
    }
    
    private void resetPinSetupInputState(EditText newPinInput, EditText confirmPinInput, TextView statusText) {
        // Reset input field colors to normal
        ColorStateList normalColor = ColorStateList.valueOf(Color.parseColor("#808080"));
        newPinInput.setBackgroundTintList(normalColor);
        confirmPinInput.setBackgroundTintList(normalColor);
        
        // Hide status text
        statusText.setVisibility(View.GONE);
    }
    
    private boolean validatePinSetup(String newPin, String confirmPin, EditText newPinInput, EditText confirmPinInput, TextView statusText) {
        if (newPin.isEmpty() || confirmPin.isEmpty()) {
            showPinSetupError("Please fill both PIN fields", newPinInput, confirmPinInput, statusText);
            return false;
        }
        
        if (newPin.length() != 4 || !newPin.matches("\\d{4}")) {
            showPinSetupError("PIN must be exactly 4 digits", newPinInput, confirmPinInput, statusText);
            return false;
        }
        
        if (!newPin.equals(confirmPin)) {
            showPinSetupError("PINs don't match", newPinInput, confirmPinInput, statusText);
            return false;
        }
        
        return true;
    }
    
    private void showPinSetupError(String message, EditText newPinInput, EditText confirmPinInput, TextView statusText) {
        // Set input field colors to red
        ColorStateList errorColor = ColorStateList.valueOf(Color.parseColor("#FF6B6B"));
        newPinInput.setBackgroundTintList(errorColor);
        confirmPinInput.setBackgroundTintList(errorColor);
        
        // Show error message in status text
        statusText.setText(message);
        statusText.setTextColor(Color.parseColor("#FF6B6B"));
        statusText.setVisibility(View.VISIBLE);
    }
    
    private void showPinSetupSuccess(TextView statusText) {
        statusText.setText("✅ PIN set successfully!");
        statusText.setTextColor(Color.parseColor("#4CAF50"));
        statusText.setVisibility(View.VISIBLE);
    }
    
    private void setupCurrentPinInputWatcher(EditText pinInput, TextView statusText) {
        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetCurrentPinInputState(pinInput, statusText);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        };
        
        pinInput.addTextChangedListener(inputWatcher);
    }
    
    private void resetCurrentPinInputState(EditText pinInput, TextView statusText) {
        // Reset input field color to normal
        ColorStateList normalColor = ColorStateList.valueOf(Color.parseColor("#808080"));
        pinInput.setBackgroundTintList(normalColor);
        
        // Hide status text
        statusText.setVisibility(View.GONE);
    }
    
    private boolean validateCurrentPin(String enteredPin, EditText pinInput, TextView statusText) {
        if (enteredPin.isEmpty()) {
            showCurrentPinError("Please enter your current PIN", pinInput, statusText);
            return false;
        }
        
        if (enteredPin.length() != 4 || !enteredPin.matches("\\d{4}")) {
            showCurrentPinError("Please enter a 4-digit PIN", pinInput, statusText);
            return false;
        }
        
        String currentPin = preferences.getString("unlock_pin", "");
        if (!currentPin.equals(enteredPin)) {
            showCurrentPinError("Wrong PIN! Check again.", pinInput, statusText);
            return false;
        }
        
        return true;
    }
    
    private void showCurrentPinError(String message, EditText pinInput, TextView statusText) {
        // Set input field color to red
        ColorStateList errorColor = ColorStateList.valueOf(Color.parseColor("#FF6B6B"));
        pinInput.setBackgroundTintList(errorColor);
        
        // Show error message in status text
        statusText.setText(message);
        statusText.setTextColor(Color.parseColor("#FF6B6B"));
        statusText.setVisibility(View.VISIBLE);
    }
}
