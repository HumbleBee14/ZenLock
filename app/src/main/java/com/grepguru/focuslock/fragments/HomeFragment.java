package com.grepguru.focuslock.fragments;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grepguru.focuslock.LockScreenActivity;
import com.grepguru.focuslock.R;
import com.grepguru.focuslock.utils.AnalyticsManager;

public class HomeFragment extends Fragment {

    private NumberPicker hoursPicker, minutesPicker;
    private TextView selectedTimeDisplay;
    private Button enableLockButton;
    private int selectedHours = 0, selectedMinutes = 0;
    private AnalyticsManager analyticsManager;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        hoursPicker = view.findViewById(R.id.hoursPicker);
        minutesPicker = view.findViewById(R.id.minutesPicker);
        selectedTimeDisplay = view.findViewById(R.id.selectedTimeDisplay);
        enableLockButton = view.findViewById(R.id.enableLockButton);

        setupNumberPickers();
        setupPresetButtons(view);
        enableLockButton.setOnClickListener(v -> checkAndStartLockService());
        
        // Initialize analytics manager
        analyticsManager = new AnalyticsManager(requireContext());

        return view;
    }

    

    private void setupNumberPickers() {
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(8); // Updated for new values
        String[] minuteValues = {"0", "1", "5", "10", "15", "20", "30", "40", "50"}; // Added 5 & 15
        minutesPicker.setDisplayedValues(minuteValues);

        hoursPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSelectedTime());
        minutesPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSelectedTime());

        // Default values
        hoursPicker.setValue(0);
        minutesPicker.setValue(1);
        updateSelectedTime();
    }

    private void setupPresetButtons(View view) {
        Button preset30min = view.findViewById(R.id.preset30min);
        Button preset1hour = view.findViewById(R.id.preset1hour);
        Button preset3hour = view.findViewById(R.id.preset3hour);

        preset30min.setOnClickListener(v -> setPresetTime(0, 30));
        preset1hour.setOnClickListener(v -> setPresetTime(1, 0));
        preset3hour.setOnClickListener(v -> setPresetTime(3, 0));
    }

    private void setPresetTime(int hours, int minutes) {
        hoursPicker.setValue(hours);
        
        // Convert minutes to picker index with new values
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        int minuteIndex = 0;
        for (int i = 0; i < minuteValues.length; i++) {
            if (minuteValues[i] == minutes) {
                minuteIndex = i;
                break;
            }
        }
        minutesPicker.setValue(minuteIndex);
        
        updateSelectedTime();
    }

    private void updateSelectedTime() {
        selectedHours = hoursPicker.getValue();
        int minuteIndex = minutesPicker.getValue();
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        selectedMinutes = minuteValues[minuteIndex];

        if (selectedHours == 0 && selectedMinutes == 0) {
            selectedTimeDisplay.setText("Set a time to start Focus Lock");
            enableLockButton.setEnabled(false);
            enableLockButton.setAlpha(0.5f);
        } else {
            selectedTimeDisplay.setText("Lock Time: " + selectedHours + " hrs " + selectedMinutes + " mins");
            enableLockButton.setEnabled(true);
            enableLockButton.setAlpha(1f);
        }
    }

    private void checkAndStartLockService() {
        if (!isAccessibilityPermissionGranted()) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(getActivity(), "Please enable Accessibility for Focus Lock", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        long lockDurationMillis = (selectedHours * 3600 + selectedMinutes * 60) * 1000;

        if (lockDurationMillis <= 0) {
            Toast.makeText(getActivity(), "Please select a valid lock time!", Toast.LENGTH_SHORT).show();
            return;
        }

        long lockEndTime = System.currentTimeMillis() + lockDurationMillis;
        long uptimeAtLock = android.os.SystemClock.elapsedRealtime();

        editor.putBoolean("isLocked", true);
        editor.putLong("lockEndTime", lockEndTime);
        editor.putLong("uptimeAtLock", uptimeAtLock);  // Store uptime to detect restarts (CRITICAL
        editor.putBoolean("wasDeviceRestarted", false);
        editor.apply();

        // Start analytics tracking
        analyticsManager.startSession(lockDurationMillis);

        // Start LockScreenActivity with selected duration
        Intent intent = new Intent(getActivity(), LockScreenActivity.class);
        intent.putExtra("lockDuration", lockDurationMillis);
        startActivity(intent);
    }

    private boolean isAccessibilityPermissionGranted() {
        AccessibilityManager am = (AccessibilityManager) requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (AccessibilityServiceInfo service : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (service.getId().contains(requireContext().getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

}