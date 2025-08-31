package com.grepguru.focuslock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.grepguru.focuslock.R;
import com.grepguru.focuslock.WhitelistActivity;

public class SettingsFragment extends Fragment {

    private EditText pinInput;
    private SwitchCompat superStrictModeToggle, defaultAppsToggle, quotesToggle;
    private SwitchCompat persistentNotificationToggle, autoRestartToggle;
    private RadioGroup securityLevelGroup;
    private RadioButton basicSecurity, enhancedSecurity, maximumSecurity;
    
    // Expandable UI elements
    private LinearLayout lockProtectionHeader, lockProtectionContent;
    private ImageView lockProtectionExpandIcon;
    private TextView lockProtectionSummary;
    private SharedPreferences preferences;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize components
        preferences = requireActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);

        pinInput = view.findViewById(R.id.pinInput);
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

        // Load existing settings
        pinInput.setText("");
        pinInput.setText(preferences.getString("lock_pin", ""));
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

        // Set up event listeners
        setupListeners(view);

        return view;
    }

    private void setupListeners(View view) {
        Button saveButton = view.findViewById(R.id.saveButton);
        Button setPinButton = view.findViewById(R.id.setPinButton);
        Button whitelistButton = view.findViewById(R.id.whitelistButton);

        // Save Button Click Listener
        saveButton.setOnClickListener(v -> saveSettings());

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

        // Partner Contact Configuration
        Button accountabilityPartnerButton = view.findViewById(R.id.accountabilityPartnerButton);
        
        // Enable the button and launch PartnerContactActivity
        accountabilityPartnerButton.setEnabled(true);
        accountabilityPartnerButton.setAlpha(1.0f);
        accountabilityPartnerButton.setText("Configure Partner Contact");
        
        accountabilityPartnerButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.grepguru.focuslock.PartnerContactActivity.class);
            startActivity(intent);
        });

        setPinButton.setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();

            if (pin.isEmpty()) {
                // Show confirmation dialog to clear PIN
                new AlertDialog.Builder(requireContext())
                        .setTitle("Clear PIN?")
                        .setMessage("Are you sure you want to remove the PIN? Lock screen will not require a PIN if cleared!")
                        .setPositiveButton("Yes, Clear", (dialog, which) -> {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.remove("lock_pin");
                            editor.apply();

                            Toast.makeText(getActivity(), "PIN Removed Successfully!", Toast.LENGTH_SHORT).show();
                            pinInput.setText("");
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            } else if (pin.length() < 4) {
                Toast.makeText(getActivity(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            } else if (!pin.matches("\\d{4}")) {
                Toast.makeText(getActivity(), "PIN must contain only numbers", Toast.LENGTH_SHORT).show();
            } else {
                // Save the PIN
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("lock_pin", pin);
                editor.apply();

                Toast.makeText(getActivity(), "PIN Set Successfully!", Toast.LENGTH_SHORT).show();
                pinInput.setText("");
            }
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("super_strict_mode", superStrictModeToggle.isChecked());
        editor.putBoolean("allow_default_apps", defaultAppsToggle.isChecked());
        editor.putBoolean("show_quotes", quotesToggle.isChecked());
        editor.apply();

        Toast.makeText(getActivity(), "Settings Saved!", Toast.LENGTH_SHORT).show();
        pinInput.setText("");

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HomeFragment())
                .commit();
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
        pinInput.setText("");
    }
}
