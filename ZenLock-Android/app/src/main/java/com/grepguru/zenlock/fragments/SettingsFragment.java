package com.grepguru.zenlock.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.grepguru.zenlock.permissions.AppPermission;
import com.grepguru.zenlock.permissions.FeaturePermissions;
import com.grepguru.zenlock.utils.NotificationPermissionManager;
import com.grepguru.zenlock.permissions.PermissionGate;
import com.grepguru.zenlock.guards.PinUnlock;
import com.grepguru.zenlock.quotes.QuotesSheet;
import com.grepguru.zenlock.ui.ConfirmSheet;
import com.grepguru.zenlock.ui.Sheets;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.WhitelistActivity;

public class SettingsFragment extends Fragment {

    private SwitchCompat autoRestartToggle, vibrationToggle, blockNotificationsToggle;
    private SwitchCompat quotesToggle, circularTimerToggle, persistentNotificationToggle;
    private ImageView addQuoteButton;

    // Individual default app toggles
    private SwitchCompat phoneAppToggle, calendarAppToggle, clockAppToggle;
    
    // Default Apps expandable UI elements
    private LinearLayout defaultAppsHeader, defaultAppsExpandableContent;
    private ImageView defaultAppsExpandIcon;
    
    private SharedPreferences preferences;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) preferences.edit().putBoolean("persistent_notification", true).apply();
                syncNotificationToggle();
            });
    
    private TextView partnerConfigValue;
    private SwitchCompat pinUnlockToggle, partnerUnlockToggle;
    private com.google.android.material.button.MaterialButton clearPinButton;
    private LinearLayout partnerConfigSection;
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

        // Default Apps expandable UI elements
        defaultAppsHeader = view.findViewById(R.id.defaultAppsHeader);
        defaultAppsExpandableContent = view.findViewById(R.id.defaultAppsExpandableContent);
        defaultAppsExpandIcon = view.findViewById(R.id.defaultAppsExpandIcon);

        partnerConfigValue = view.findViewById(R.id.partnerConfigValue);
        
        // Toggle switches for unlock methods
        pinUnlockToggle = view.findViewById(R.id.pinUnlockToggle);
        clearPinButton = view.findViewById(R.id.clearPinButton);
        partnerUnlockToggle = view.findViewById(R.id.partnerUnlockToggle);
        
        // Individual default app toggles
        phoneAppToggle = view.findViewById(R.id.phoneAppToggle);
        calendarAppToggle = view.findViewById(R.id.calendarAppToggle);
        clockAppToggle = view.findViewById(R.id.clockAppToggle);
        
        // Expandable sections
        partnerConfigSection = view.findViewById(R.id.partnerConfigSection);
        
        // Warning message
        noUnlockMethodsWarning = view.findViewById(R.id.noUnlockMethodsWarning);

        // Feedback and Support Cards
        View feedbackCard = view.findViewById(R.id.feedbackCard);
        View supportDeveloperCard = view.findViewById(R.id.supportDeveloperCard);

        // Load existing settings
        quotesToggle = view.findViewById(R.id.quotesToggle);
        quotesToggle.setChecked(preferences.getBoolean("show_quotes", true));
        addQuoteButton = view.findViewById(R.id.addQuoteButton);
        addQuoteButton.setVisibility(quotesToggle.isChecked() ? View.VISIBLE : View.GONE);
        addQuoteButton.setOnClickListener(v -> QuotesSheet.show(requireActivity()));
        circularTimerToggle = view.findViewById(R.id.circularTimerToggle);
        circularTimerToggle.setChecked("circular".equals(preferences.getString("timer_style", "digital")));
        
        // Load individual default app settings
        phoneAppToggle.setChecked(preferences.getBoolean("allow_phone_app", true));
        calendarAppToggle.setChecked(preferences.getBoolean("allow_calendar_app", true));
        clockAppToggle.setChecked(preferences.getBoolean("allow_clock_app", true));
        
        // Load security settings
        persistentNotificationToggle = view.findViewById(R.id.persistentNotificationToggle);
        syncNotificationToggle();
        
        // Load auto-restart setting
        autoRestartToggle.setChecked(preferences.getBoolean("auto_restart", true));
        
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

        // Block Notifications toggle (default ON)
        blockNotificationsToggle = view.findViewById(R.id.blockNotificationsToggle);
        syncBlockNotificationsToggle();
        blockNotificationsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !AppPermission.NOTIFICATION_ACCESS.isGranted(requireContext())) {
                blockNotificationsToggle.setChecked(false);
                PermissionGate.ensure(requireActivity(), FeaturePermissions.notificationBlocking(), () -> {
                    if (!isAdded()) return;
                    preferences.edit().putBoolean("block_notifications", true).apply();
                    syncBlockNotificationsToggle();
                });
                return;
            }
            preferences.edit().putBoolean("block_notifications", isChecked).apply();
        });

        // Allow Launcher/Home Screen during Lock toggle
        allowLauncherToggle = view.findViewById(R.id.allowLauncherToggle);
        allowLauncherToggle.setChecked(preferences.getBoolean("allow_launcher_during_lock", false));
        allowLauncherToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                preferences.edit().putBoolean("allow_launcher_during_lock", false).apply();
                return;
            }
            if (preferences.getBoolean("allow_launcher_during_lock", false)) return;
            allowLauncherToggle.setChecked(false);
            ConfirmSheet.show(requireActivity(), "Allow home screen?",
                    "Not recommended. Reaching the launcher makes it easy to slip into other apps mid-session.",
                    "Keep off", "Allow anyway", () -> {
                        if (!isAdded()) return;
                        preferences.edit().putBoolean("allow_launcher_during_lock", true).apply();
                        allowLauncherToggle.setChecked(true);
                    });
        });

        TextView versionText = view.findViewById(R.id.versionText);
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            versionText.setText("ZenLock " + versionName);
        } catch (Exception e) {
            versionText.setVisibility(View.GONE);
        }

        return view;
    }

    private void setupListeners(View view) {
        View whitelistButton = view.findViewById(R.id.whitelistButton);
        view.findViewById(R.id.permissionsRow).setOnClickListener(v -> PermissionGate.review(requireActivity()));

        // Feedback and Support Card Listeners
        View feedbackCard = view.findViewById(R.id.feedbackCard);
        View supportDeveloperCard = view.findViewById(R.id.supportDeveloperCard);

        feedbackCard.setOnClickListener(v -> openFeedbackEmail());
        supportDeveloperCard.setOnClickListener(v -> openSupportPage());


        // Toggle Motivational Quotes
        quotesToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("show_quotes", isChecked).apply();
            addQuoteButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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
            if (isChecked && !NotificationPermissionManager.hasNotificationPermission(requireContext())) {
                requestNotificationPermission();
                return;
            }
            preferences.edit().putBoolean("persistent_notification", isChecked).apply();
        });

        // Setup expandable functionality
        setupExpandableSections();

        // Navigate to Whitelist Management
        whitelistButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), WhitelistActivity.class);
            startActivity(intent);
        });

        updateBatteryExemptionState(view);

        // PIN Unlock Toggle
        pinUnlockToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !PinUnlock.isConfigured(requireContext())) {
                showPinSheet();
                return;
            }
            PinUnlock.setEnabled(requireContext(), isChecked);
            updateUnlockMethodStates();
        });
        clearPinButton.setOnClickListener(v -> {
            PinUnlock.clear(requireContext());
            updateUnlockMethodStates();
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
                editor.remove("enable_sms_notifications");
                editor.apply();
                updateUnlockMethodStates();
            }
        });

        // PIN Configuration Listeners
        // Partner Contact Configuration
        partnerConfigSection.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.grepguru.zenlock.PartnerContactActivity.class);
            startActivity(intent);
        });
    }

    private void initializeToggleStates() {
        pinUnlockToggle.setChecked(PinUnlock.isEnabled(requireContext()));
    }

    private void updateUnlockMethodStates() {
        boolean pinConfigured = PinUnlock.isEnabled(requireContext());
        pinUnlockToggle.setChecked(pinConfigured);
        clearPinButton.setVisibility(PinUnlock.isConfigured(requireContext()) ? View.VISIBLE : View.GONE);

        // Check Partner status
        String partnerPhone = preferences.getString("partner_phone", "");
        boolean partnerConfigured = !partnerPhone.isEmpty();
        
        partnerUnlockToggle.setChecked(partnerConfigured);
        partnerConfigSection.setVisibility(partnerConfigured ? View.VISIBLE : View.GONE);
        
        partnerConfigValue.setText(partnerConfigured ? "Update" : "Configure");
        
        // Show/hide warning message based on unlock method availability
        boolean hasAnyUnlockMethod = pinConfigured || partnerConfigured;
        noUnlockMethodsWarning.setVisibility(hasAnyUnlockMethod ? View.GONE : View.VISIBLE);
    }

    private void setupExpandableSections() {
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

    private void syncBlockNotificationsToggle() {
        boolean enabled = preferences.getBoolean("block_notifications", true)
                && AppPermission.NOTIFICATION_ACCESS.isGranted(requireContext());
        blockNotificationsToggle.setChecked(enabled);
    }

    private void syncNotificationToggle() {
        boolean enabled = preferences.getBoolean("persistent_notification", true)
                && NotificationPermissionManager.hasNotificationPermission(requireContext());
        persistentNotificationToggle.setChecked(enabled);
    }

    private void requestNotificationPermission() {
        if (NotificationPermissionManager.isPermissionPermanentlyDenied(requireActivity())) {
            Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
            startActivity(intent);
            return;
        }
        notificationPermissionLauncher.launch(NotificationPermissionManager.NOTIFICATION_PERMISSION);
    }

    private void updateBatteryExemptionState(View view) {
        SwitchCompat toggle = view.findViewById(R.id.batteryExemptionToggle);
        if (toggle == null) return;
        boolean exempt = com.grepguru.zenlock.utils.BatteryOptimizationManager.isExempt(requireContext());
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(exempt);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                com.grepguru.zenlock.utils.BatteryOptimizationManager.requestExemption(requireContext());
            } else {
                openBatteryOptimizationSettings();
            }
        });
    }

    private void openBatteryOptimizationSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (Exception e) {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        }
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
        syncNotificationToggle();
        syncBlockNotificationsToggle();
        if (getView() != null) {
            updateBatteryExemptionState(getView());
        }

    }
    
    
    private void showPinSheet() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trusted_pin_setup, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        dialog.setContentView(dialogView);

        EditText newPinInput = dialogView.findViewById(R.id.newPinInput);
        EditText confirmPinInput = dialogView.findViewById(R.id.confirmPinInput);
        TextView instructionText = dialogView.findViewById(R.id.instructionText);
        TextView pinStatusText = dialogView.findViewById(R.id.pinStatusText);

        setupPinSetupInputWatcher(newPinInput, confirmPinInput, pinStatusText);

        if (PinUnlock.isConfigured(requireContext())) {
            instructionText.setText("Enter a new PIN twice to replace the current one.");
        } else {
            instructionText.setText("Ask someone you trust to set it so you can't undo it on a whim.");
        }

        dialogView.findViewById(R.id.pinCancelButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.pinSaveButton).setOnClickListener(v -> {
            String newPin = newPinInput.getText().toString().trim();
            String confirmPin = confirmPinInput.getText().toString().trim();
            if (validatePinSetup(newPin, confirmPin, newPinInput, confirmPinInput, pinStatusText)) {
                PinUnlock.save(requireContext(), newPin);
                updateUnlockMethodStates();
                dialog.dismiss();
                Toast.makeText(requireContext(), "PIN set", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnDismissListener(d -> updateUnlockMethodStates());
        dialog.setOnShowListener(d -> Sheets.expandAboveKeyboard(dialog));
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
        statusText.setVisibility(View.GONE);
    }
    
    private boolean validatePinSetup(String newPin, String confirmPin, EditText newPinInput, EditText confirmPinInput, TextView statusText) {
        if (newPin.isEmpty() || confirmPin.isEmpty()) {
            showPinSetupError("Enter the PIN in both fields", newPinInput, confirmPinInput, statusText);
            return false;
        }
        
        if (newPin.length() != 4 || !newPin.matches("\\d{4}")) {
            showPinSetupError("PIN must be 4 digits", newPinInput, confirmPinInput, statusText);
            return false;
        }
        
        if (!newPin.equals(confirmPin)) {
            showPinSetupError("PINs don't match", newPinInput, confirmPinInput, statusText);
            return false;
        }
        
        return true;
    }
    
    private void showPinSetupError(String message, EditText newPinInput, EditText confirmPinInput, TextView statusText) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
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
                "Hi Dinesh! I'd like to share some feedback about ZenLock:\n\n" +
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
            String supportUrl = "https://github.com/sponsors/HumbleBee14";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open browser. Please visit: github.com/sponsors/HumbleBee14", Toast.LENGTH_LONG).show();
        }
    }
}
