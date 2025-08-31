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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.grepguru.focuslock.R;
import com.grepguru.focuslock.WhitelistActivity;

public class SettingsFragment extends Fragment {

    private EditText pinInput;
    private SwitchCompat superStrictModeToggle, defaultAppsToggle;
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

        // Load existing settings
        pinInput.setText("");
        pinInput.setText(preferences.getString("lock_pin", ""));
        superStrictModeToggle.setChecked(preferences.getBoolean("super_strict_mode", false));
        defaultAppsToggle.setChecked(preferences.getBoolean("allow_default_apps", true));

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
        editor.apply();

        Toast.makeText(getActivity(), "Settings Saved!", Toast.LENGTH_SHORT).show();
        pinInput.setText("");

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HomeFragment())
                .commit();
    }


    @Override
    public void onResume() {
        super.onResume();
        pinInput.setText("");
    }
}
