package com.grepguru.zenlock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.WhitelistActivity;

public class SettingsFragment extends Fragment {

    private SwitchCompat autoRestartToggle, vibrationToggle;
    private SwitchCompat quotesToggle, circularTimerToggle, persistentNotificationToggle;

    // Individual default app toggles
    private SwitchCompat phoneAppToggle, calendarAppToggle, clockAppToggle;
    
    private RadioGroup securityLevelGroup;
    private RadioButton basicSecurity, enhancedSecurity, maximumSecurity;
    
    // Expandable UI elements
    private LinearLayout lockProtectionHeader, lockProtectionContent;
    private ImageView lockProtectionExpandIcon;
    private TextView lockProtectionSummary;
    
    // Default Apps expandable UI elements
    private LinearLayout defaultAppsHeader, defaultAppsExpandableContent;
    private ImageView defaultAppsExpandIcon;
    
    private SharedPreferences preferences;
    
    // Enhanced unlock UI elements
    private Button configurePinButton, clearPinButton;
    private Button accountabilityPartnerButton;
    private SwitchCompat pinUnlockToggle, partnerUnlockToggle;
    private LinearLayout pinConfigSection, partnerConfigSection;
    private LinearLayout noUnlockMethodsWarning;

    // Allow Launcher/Home Screen during Lock toggle
    private SwitchCompat allowLauncherToggle;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize components
        preferences = requireActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);

        autoRestartToggle = view.findViewById(R.id.autoRestartToggle);
        autoRestartToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("auto_restart", isChecked);
            editor.apply();
        });

        // Security settings
        securityLevelGroup = view.findViewById(R.id.securityLevelGroup);
        basicSecurity = view.findViewById(R.id.basicSecurity);
        enhancedSecurity = view.findViewById(R.id.enhancedSecurity);
        maximumSecurity = view.findViewById(R.id.maximumSecurity);
        
        // Expandable UI elements
        lockProtectionHeader = view.findViewById(R.id.lockProtectionHeader);
        lockProtectionContent = view.findViewById(R.id.lockProtectionContent);
        lockProtectionExpandIcon = view.findViewById(R.id.lockProtectionExpandIcon);
        lockProtectionSummary = view.findViewById(R.id.lockProtectionSummary);

        // Default Apps expandable UI elements
        defaultAppsHeader = view.findViewById(R.id.defaultAppsHeader);
        defaultAppsExpandableContent = view.findViewById(R.id.defaultAppsExpandableContent);
        defaultAppsExpandIcon = view.findViewById(R.id.defaultAppsExpandIcon);

        // Enhanced unlock UI elements
        configurePinButton = view.findViewById(R.id.configurePinButton);
        clearPinButton = view.findViewById(R.id.clearPinButton);
        accountabilityPartnerButton = view.findViewById(R.id.accountabilityPartnerButton);
        
        // Toggle switches for unlock methods
        pinUnlockToggle = view.findViewById(R.id.pinUnlockToggle);
        partnerUnlockToggle = view.findViewById(R.id.partnerUnlockToggle);
        
        // Individual default app toggles
        phoneAppToggle = view.findViewById(R.id.phoneAppToggle);
        calendarAppToggle = view.findViewById(R.id.calendarAppToggle);
        clockAppToggle = view.findViewById(R.id.clockAppToggle);
        
        // Expandable sections
        pinConfigSection = view.findViewById(R.id.pinConfigSection);
        partnerConfigSection = view.findViewById(R.id.partnerConfigSection);
        
        // Warning message
        noUnlockMethodsWarning = view.findViewById(R.id.noUnlockMethodsWarning);

        // Feedback and Support Cards
        View feedbackCard = view.findViewById(R.id.feedbackCard);
        View supportDeveloperCard = view.findViewById(R.id.supportDeveloperCard);

        // Load existing settings
        quotesToggle = view.findViewById(R.id.quotesToggle);
        quotesToggle.setChecked(preferences.getBoolean("show_quotes", true));
        circularTimerToggle = view.findViewById(R.id.circularTimerToggle);
        circularTimerToggle.setChecked("circular".equals(preferences.getString("timer_style", "digital")));
        
        // Load individual default app settings
        phoneAppToggle.setChecked(preferences.getBoolean("allow_phone_app", true));
        calendarAppToggle.setChecked(preferences.getBoolean("allow_calendar_app", true));
        clockAppToggle.setChecked(preferences.getBoolean("allow_clock_app", true));
        
        // Load security settings
        persistentNotificationToggle = view.findViewById(R.id.persistentNotificationToggle);
        persistentNotificationToggle.setChecked(preferences.getBoolean("persistent_notification", true));
        
        // Load auto-restart setting
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

        // Initialize toggle states based on existing configuration
        initializeToggleStates();
        
        // Update PIN button states
        updateUnlockMethodStates();

        // Set up event listeners
        setupListeners(view);

        // Vibration toggle setup (now in General Settings)
        vibrationToggle = view.findViewById(R.id.vibrationToggle);
        vibrationToggle.setChecked(com.grepguru.zenlock.VibrationUtils.isVibrationEnabled(requireContext()));
        vibrationToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.grepguru.zenlock.VibrationUtils.setVibrationEnabled(requireContext(), isChecked);
        });

        // Allow Launcher/Home Screen during Lock toggle
        allowLauncherToggle = view.findViewById(R.id.allowLauncherToggle);
        allowLauncherToggle.setChecked(preferences.getBoolean("allow_launcher_during_lock", false));
        allowLauncherToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("allow_launcher_during_lock", isChecked);
            editor.apply();
        });

        return view;
    }

    private void setupListeners(View view) {
        Button whitelistButton = view.findViewById(R.id.whitelistButton);

        // Feedback and Support Card Listeners
        View feedbackCard = view.findViewById(R.id.feedbackCard);
        View supportDeveloperCard = view.findViewById(R.id.supportDeveloperCard);

        feedbackCard.setOnClickListener(v -> openFeedbackEmail());
        supportDeveloperCard.setOnClickListener(v -> openSupportPage());


        // Toggle Motivational Quotes
        quotesToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("show_quotes", isChecked);
            editor.apply();
        });

        // Toggle Circular Timer
        circularTimerToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("timer_style", isChecked ? "circular" : "digital");
            editor.apply();
        });
        
        // Individual Default App Toggles
        phoneAppToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("allow_phone_app", isChecked);
            editor.apply();
        });
        
        calendarAppToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("allow_calendar_app", isChecked);
            editor.apply();
        });
        
        clockAppToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("allow_clock_app", isChecked);
            editor.apply();
        });
        
        // Security settings listeners
        persistentNotificationToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("persistent_notification", isChecked);
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
                // Update button states when section becomes visible
                updateUnlockMethodStates();
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
            Intent intent = new Intent(requireActivity(), com.grepguru.zenlock.PartnerContactActivity.class);
            startActivity(intent);
        });
    }

    private void initializeToggleStates() {
        // Set initial toggle states based on existing configuration
        String existingPin = preferences.getString("unlock_pin", "");
        boolean pinConfigured = !existingPin.isEmpty();
        
        // Set toggle state based on PIN existence (only on initial load)
        pinUnlockToggle.setChecked(pinConfigured);
    }

    private void updateUnlockMethodStates() {
        // Check PIN status
        String existingPin = preferences.getString("unlock_pin", "");
        boolean pinConfigured = !existingPin.isEmpty();
        
        // Update section visibility based on toggle state, not PIN existence
        boolean toggleEnabled = pinUnlockToggle.isChecked();
        pinConfigSection.setVisibility(toggleEnabled ? View.VISIBLE : View.GONE);
        
        if (pinConfigured) {
            configurePinButton.setVisibility(View.GONE);
            clearPinButton.setVisibility(View.VISIBLE);
        } else {
            configurePinButton.setVisibility(View.VISIBLE);
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

        // Default Apps expandable section
        defaultAppsHeader.setOnClickListener(v -> {
            boolean isExpanded = defaultAppsExpandableContent.getVisibility() == View.VISIBLE;
            
            if (isExpanded) {
                // Collapse
                defaultAppsExpandableContent.setVisibility(View.GONE);
                defaultAppsExpandIcon.animate()
                    .rotation(0)
                    .setDuration(200)
                    .start();
            } else {
                // Expand
                defaultAppsExpandableContent.setVisibility(View.VISIBLE);
                defaultAppsExpandIcon.animate()
                    .rotation(180)
                    .setDuration(200)
                    .start();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Enforce lock: if locked, redirect to lock screen and prevent access
        SharedPreferences preferences = requireActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);
        if (isLocked) {
            Intent lockIntent = new Intent(requireContext(), com.grepguru.zenlock.LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
            requireActivity().finish();
            return;
        }
        updateUnlockMethodStates();
    }
    
    private void showPinSetupDialog() {
        // Always show PIN setup dialog (user can clear existing PIN first if needed)
        showActualPinDialog();
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
            instructionText.setText("💡 Tip: Consider asking a trusted friend to set this PIN for you to increase accountability.");
        }
        
        builder.setView(dialogView)
//               .setTitle("PIN Setup")
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
                    
                    // Close current dialog immediately
                    dialog.dismiss();
                    
                    // Show success dialog
                    showPinSetupSuccessDialog();
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
    
    private void showPinSetupSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pin_success, null);
        
        builder.setView(dialogView)
               .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
               .setCancelable(false);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    

    /**
     * Opens email app for sending feedback to developer
     */
    private void openFeedbackEmail() {
        try {
            // Get app version dynamically
            String appVersion = "Unknown";
            try {
                appVersion = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            } catch (Exception versionError) {
                appVersion = "1.0"; // Fallback if version detection fails
            }
            
            // Use ACTION_SENDTO with mailto: for better email app filtering
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:")); // Only email apps can handle this
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"idineshy@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "ZenLock - Feedback & Suggestions");
            emailIntent.putExtra(Intent.EXTRA_TEXT, 
                "Hi! I'd like to share some feedback about ZenLock:\n\n" +
                "App Version: " + appVersion + "\n" +
                "Android Version: " + android.os.Build.VERSION.RELEASE + "\n" +
                "Device: " + android.os.Build.MODEL + "\n\n" +
                "My feedback:\n");
            
            if (emailIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(emailIntent); // No chooser needed since only email apps will show
            } else {
                Toast.makeText(getContext(), "No email app found.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open email app.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Opens support page in web browser
     */
    private void openSupportPage() {
        try {
            String supportUrl = "https://buymeacoffee.com/humblebee";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open browser. Please visit: buymeacoffee.com/humblebee", Toast.LENGTH_LONG).show();
        }
    }
}
