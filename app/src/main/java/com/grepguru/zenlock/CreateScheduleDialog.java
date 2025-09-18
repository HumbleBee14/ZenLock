package com.grepguru.zenlock;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.grepguru.zenlock.model.ScheduleModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Dialog for creating and editing schedules
 * Features: Time picker, focus duration, repeat options, pre-notify settings
 */
public class CreateScheduleDialog extends DialogFragment {
    
    private ScheduleListener listener;
    private ScheduleModel scheduleToEdit;
    
    // UI Components
    private EditText nameInput;
    private TextView startTimeText;
    private TextView selectedDurationDisplay;
    private TextView selectedFrequencyDisplay;
    private LinearLayout frequencySelectionLayout;
    private RadioGroup repeatTypeGroup;
    private LinearLayout weeklyDaysLayout;
    private CheckBox[] dayCheckboxes;
    private CheckBox preNotifyCheckbox;
    private Spinner preNotifySpinner;
    private Button createButton;
    private Button cancelButton;
    
    // Data
    private int selectedHour = 9;
    private int selectedMinute = 0;
    private int selectedDurationHours = 0;
    private int selectedDurationMinutes = 1;
    
    public interface ScheduleListener {
        void onScheduleCreated(ScheduleModel schedule);
    }
    
    public void setScheduleListener(ScheduleListener listener) {
        this.listener = listener;
    }
    
    public void setScheduleToEdit(ScheduleModel schedule) {
        this.scheduleToEdit = schedule;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_schedule, container, false);
        
        initializeViews(view);
        setupListeners();
        
        if (scheduleToEdit != null) {
            populateForEdit();
        } else {
            updateStartTimeDisplay();
            updateDurationDisplay();
            updateFrequencyDisplay();
        }
        
        return view;
    }
    
    private void initializeViews(View view) {
        nameInput = view.findViewById(R.id.nameInput);
        startTimeText = view.findViewById(R.id.startTimeText);
        selectedDurationDisplay = view.findViewById(R.id.selectedDurationDisplay);
        selectedFrequencyDisplay = view.findViewById(R.id.selectedFrequencyDisplay);
        frequencySelectionLayout = view.findViewById(R.id.frequencySelectionLayout);
        repeatTypeGroup = view.findViewById(R.id.repeatTypeGroup);
        weeklyDaysLayout = view.findViewById(R.id.weeklyDaysLayout);
        preNotifyCheckbox = view.findViewById(R.id.preNotifyCheckbox);
        preNotifySpinner = view.findViewById(R.id.preNotifySpinner);
        createButton = view.findViewById(R.id.createButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        
        // Initialize day checkboxes
        dayCheckboxes = new CheckBox[7];
        dayCheckboxes[0] = view.findViewById(R.id.sundayCheckbox);
        dayCheckboxes[1] = view.findViewById(R.id.mondayCheckbox);
        dayCheckboxes[2] = view.findViewById(R.id.tuesdayCheckbox);
        dayCheckboxes[3] = view.findViewById(R.id.wednesdayCheckbox);
        dayCheckboxes[4] = view.findViewById(R.id.thursdayCheckbox);
        dayCheckboxes[5] = view.findViewById(R.id.fridayCheckbox);
        dayCheckboxes[6] = view.findViewById(R.id.saturdayCheckbox);
    }
    
    private void setupListeners() {
        // Start time picker
        startTimeText.setOnClickListener(v -> showTimePicker());
        
        // Focus duration picker
        selectedDurationDisplay.setOnClickListener(v -> showDurationPicker());
        
        // Frequency selection
        selectedFrequencyDisplay.setOnClickListener(v -> toggleFrequencySelection());
        
        // Repeat type change
        repeatTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.weeklyRadio) {
                weeklyDaysLayout.setVisibility(View.VISIBLE);
            } else {
                weeklyDaysLayout.setVisibility(View.GONE);
            }
            updateFrequencyDisplay();
            frequencySelectionLayout.setVisibility(View.GONE);
        });
        
        // Pre-notify checkbox
        preNotifyCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preNotifySpinner.setEnabled(isChecked);
        });
        
        // Setup custom spinner adapter for proper text colors
        setupSpinnerAdapter();
        
        // Create button
        createButton.setOnClickListener(v -> createSchedule());
        
        // Cancel button
        cancelButton.setOnClickListener(v -> dismiss());
    }
    
    private void showDurationPicker() {
        // Create a custom dialog with NumberPickers
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_duration_picker, null);
        
        NumberPicker hoursPicker = dialogView.findViewById(R.id.hoursPicker);
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);
        
        // Setup pickers
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(8);
        String[] minuteValues = {"0", "1", "5", "10", "15", "20", "30", "40", "50"};
        minutesPicker.setDisplayedValues(minuteValues);
        
        // Set current values
        hoursPicker.setValue(selectedDurationHours);
        minutesPicker.setValue(getMinuteIndex(selectedDurationMinutes));
        
        android.app.AlertDialog dialog = builder.setView(dialogView)
                .setPositiveButton("Set", (dialogInterface, which) -> {
                    selectedDurationHours = hoursPicker.getValue();
                    int minuteIndex = minutesPicker.getValue();
                    selectedDurationMinutes = Integer.parseInt(minuteValues[minuteIndex]);
                    updateDurationDisplay();
                })
                .setNegativeButton("Cancel", null)
                .create();
        
        // Apply custom theme to dialog
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(requireContext().getColor(R.color.primary));
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(requireContext().getColor(R.color.black));
        });
        
        dialog.show();
    }
    
    private int getMinuteIndex(int minutes) {
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        for (int i = 0; i < minuteValues.length; i++) {
            if (minuteValues[i] == minutes) {
                return i;
            }
        }
        return 1; // Default to 1 minute
    }
    
    private void updateDurationDisplay() {
        if (selectedDurationHours == 0 && selectedDurationMinutes == 0) {
            selectedDurationDisplay.setText("Set a duration for focus session");
        } else {
            selectedDurationDisplay.setText(selectedDurationHours + " hrs " + selectedDurationMinutes + " mins");
        }
    }
    
    private void toggleFrequencySelection() {
        if (frequencySelectionLayout.getVisibility() == View.VISIBLE) {
            frequencySelectionLayout.setVisibility(View.GONE);
        } else {
            frequencySelectionLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateFrequencyDisplay() {
        int checkedId = repeatTypeGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.dailyRadio) {
            selectedFrequencyDisplay.setText("Daily");
        } else if (checkedId == R.id.weeklyRadio) {
            selectedFrequencyDisplay.setText("Weekly");
        } else {
            selectedFrequencyDisplay.setText("Once");
        }
    }
    
    private void setupSpinnerAdapter() {
        String[] notifyTimes = getResources().getStringArray(R.array.pre_notify_times);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            notifyTimes
        ) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                if (view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setTextColor(requireContext().getColor(R.color.textPrimary));
                }
                return view;
            }
            
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setTextColor(requireContext().getColor(R.color.textPrimary));
                    view.setBackgroundColor(requireContext().getColor(R.color.surface));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        preNotifySpinner.setAdapter(adapter);
    }
    
    private void showTimePicker() {
        // Use Material Design Time Picker with proper theme
        com.google.android.material.timepicker.MaterialTimePicker timePicker = 
            new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H) // AM/PM format
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText("Select Time")
                .setInputMode(com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK) // Start with clock view
                .setTheme(R.style.MaterialTimePickerTheme) // Apply professional theme
                .build();
        
        timePicker.addOnPositiveButtonClickListener(view -> {
            selectedHour = timePicker.getHour();
            selectedMinute = timePicker.getMinute();
            updateStartTimeDisplay();
        });
        
        timePicker.show(getChildFragmentManager(), "MaterialTimePicker");
    }
    
    private void updateStartTimeDisplay() {
        // Format time with AM/PM for better user experience
        String amPm = selectedHour >= 12 ? "PM" : "AM";
        int displayHour = selectedHour;
        if (selectedHour == 0) {
            displayHour = 12; // 12 AM
        } else if (selectedHour > 12) {
            displayHour = selectedHour - 12; // 1-11 PM
        }
        startTimeText.setText(String.format("%d:%02d %s", displayHour, selectedMinute, amPm));
    }
    
    private void populateForEdit() {
        if (scheduleToEdit == null) return;
        
        nameInput.setText(scheduleToEdit.getName());
        selectedHour = scheduleToEdit.getStartHour();
        selectedMinute = scheduleToEdit.getStartMinute();
        updateStartTimeDisplay();
        
        // Set duration
        int totalMinutes = scheduleToEdit.getFocusDurationMinutes();
        selectedDurationHours = totalMinutes / 60;
        selectedDurationMinutes = totalMinutes % 60;
        updateDurationDisplay();
        
        // Set repeat type
        switch (scheduleToEdit.getRepeatType()) {
            case DAILY:
                repeatTypeGroup.check(R.id.dailyRadio);
                break;
            case WEEKLY:
                repeatTypeGroup.check(R.id.weeklyRadio);
                weeklyDaysLayout.setVisibility(View.VISIBLE);
                break;
            case ONCE:
                repeatTypeGroup.check(R.id.onceRadio);
                break;
        }
        updateFrequencyDisplay();
        
        // Set weekly days
        Set<Integer> repeatDays = scheduleToEdit.getRepeatDays();
        for (int i = 0; i < dayCheckboxes.length; i++) {
            dayCheckboxes[i].setChecked(repeatDays.contains(i + 1));
        }
        
        // Set pre-notify
        preNotifyCheckbox.setChecked(scheduleToEdit.isPreNotifyEnabled());
        int notifyIndex = getNotifyIndex(scheduleToEdit.getPreNotifyMinutes());
        preNotifySpinner.setSelection(notifyIndex);
        preNotifySpinner.setEnabled(scheduleToEdit.isPreNotifyEnabled());
        
        createButton.setText("Update");
    }
    

    
    private int getNotifyIndex(int minutes) {
        int[] notifyTimes = {1, 2, 3, 5, 10};
        for (int i = 0; i < notifyTimes.length; i++) {
            if (notifyTimes[i] == minutes) {
                return i;
            }
        }
        return 3; // Default to 5 minutes
    }
    
    private void createSchedule() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a schedule name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get duration from pickers
        selectedDurationMinutes = (selectedDurationHours * 60) + selectedDurationMinutes;
        
        // Get repeat type
        ScheduleModel.RepeatType repeatType;
        int checkedId = repeatTypeGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.dailyRadio) {
            repeatType = ScheduleModel.RepeatType.DAILY;
        } else if (checkedId == R.id.weeklyRadio) {
            repeatType = ScheduleModel.RepeatType.WEEKLY;
        } else {
            repeatType = ScheduleModel.RepeatType.ONCE;
        }
        
        // Get weekly days
        Set<Integer> repeatDays = new HashSet<>();
        if (repeatType == ScheduleModel.RepeatType.WEEKLY) {
            for (int i = 0; i < dayCheckboxes.length; i++) {
                if (dayCheckboxes[i].isChecked()) {
                    repeatDays.add(i + 1); // Calendar.DAY_OF_WEEK values
                }
            }
        }
        
        // Get pre-notify settings
        boolean preNotifyEnabled = preNotifyCheckbox.isChecked();
        int preNotifyMinutes = 5; // Default
        if (preNotifyEnabled) {
            int notifyIndex = preNotifySpinner.getSelectedItemPosition();
            int[] notifyTimes = {1, 2, 3, 5, 10};
            preNotifyMinutes = notifyTimes[notifyIndex];
        }
        
        // Create or update schedule
        ScheduleModel schedule;
        if (scheduleToEdit != null) {
            schedule = scheduleToEdit;
        } else {
            schedule = new ScheduleModel();
        }
        
        schedule.setName(name);
        schedule.setStartHour(selectedHour);
        schedule.setStartMinute(selectedMinute);
        schedule.setFocusDurationMinutes(selectedDurationMinutes);
        schedule.setRepeatType(repeatType);
        schedule.setRepeatDays(repeatDays);
        schedule.setPreNotifyEnabled(preNotifyEnabled);
        schedule.setPreNotifyMinutes(preNotifyMinutes);
        
        if (listener != null) {
            listener.onScheduleCreated(schedule);
        }
        
        dismiss();
    }
} 