package com.grepguru.zenlock;

import android.content.Context;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.grepguru.zenlock.model.ScheduleModel;
import com.grepguru.zenlock.ui.Sheets;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class CreateScheduleDialog extends BottomSheetDialogFragment {

    private static final int[] NOTIFY_MINUTES = {1, 2, 3, 5, 10};
    private static final int[] DURATION_MINUTE_STEPS = {0, 1, 5, 10, 15, 20, 30, 40, 50};

    private ScheduleListener listener;
    private ScheduleModel scheduleToEdit;
    private ScheduleModel template;

    private EditText nameInput;
    private TextView startTimeText;
    private TextView selectedDurationDisplay;
    private ChipChoice repeatChoice;
    private View weeklyDaysLayout;
    private Chip[] dayChips;
    private SwitchCompat preNotifySwitch;
    private View preNotifyOptions;
    private ChipChoice notifyChoice;
    private MaterialButton createButton;

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

    public void setTemplate(ScheduleModel schedule) {
        this.template = schedule;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_schedule, container, false);
        bindViews(view);
        if (scheduleToEdit != null) {
            ((TextView) view.findViewById(R.id.scheduleDialogTitle)).setText(getString(R.string.screen_edit_schedule));
            createButton.setText(getString(R.string.screen_save));
            populate(scheduleToEdit);
        } else if (template != null) {
            populate(template);
        } else {
            repeatChoice.select(1);
            notifyChoice.select(3);
            for (int i = 1; i <= 5; i++) dayChips[i].setChecked(true);
        }
        updateStartTimeDisplay();
        updateDurationDisplay();
        syncRepeatVisibility();
        preNotifyOptions.setVisibility(preNotifySwitch.isChecked() ? View.VISIBLE : View.GONE);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Sheets.expandAboveKeyboard(getDialog());
    }

    private void bindViews(View view) {
        nameInput = view.findViewById(R.id.nameInput);
        startTimeText = view.findViewById(R.id.startTimeText);
        selectedDurationDisplay = view.findViewById(R.id.selectedDurationDisplay);
        weeklyDaysLayout = view.findViewById(R.id.weeklyDaysLayout);
        preNotifySwitch = view.findViewById(R.id.preNotifySwitch);
        preNotifyOptions = view.findViewById(R.id.preNotifyOptions);
        createButton = view.findViewById(R.id.createButton);

        repeatChoice = new ChipChoice(
                view.findViewById(R.id.onceChip),
                view.findViewById(R.id.dailyChip),
                view.findViewById(R.id.weeklyChip));
        repeatChoice.onChange(this::syncRepeatVisibility);

        notifyChoice = new ChipChoice(
                view.findViewById(R.id.notify1Chip),
                view.findViewById(R.id.notify2Chip),
                view.findViewById(R.id.notify3Chip),
                view.findViewById(R.id.notify5Chip),
                view.findViewById(R.id.notify10Chip));

        dayChips = new Chip[]{
                view.findViewById(R.id.sundayChip),
                view.findViewById(R.id.mondayChip),
                view.findViewById(R.id.tuesdayChip),
                view.findViewById(R.id.wednesdayChip),
                view.findViewById(R.id.thursdayChip),
                view.findViewById(R.id.fridayChip),
                view.findViewById(R.id.saturdayChip)};

        android.icu.text.DateFormatSymbols weekdays = new android.icu.text.DateFormatSymbols(
                getResources().getConfiguration().getLocales().get(0));
        String[] narrow = weekdays.getWeekdays(android.icu.text.DateFormatSymbols.STANDALONE,
                android.icu.text.DateFormatSymbols.NARROW);
        String[] full = weekdays.getWeekdays();
        for (int i = 0; i < dayChips.length; i++) {
            dayChips[i].setText(narrow[i + 1]);
            dayChips[i].setContentDescription(full[i + 1]);
        }

        view.findViewById(R.id.startTimeRow).setOnClickListener(v -> showTimePicker());
        view.findViewById(R.id.durationRow).setOnClickListener(v -> showDurationPicker());
        preNotifySwitch.setOnCheckedChangeListener((button, checked) ->
                preNotifyOptions.setVisibility(checked ? View.VISIBLE : View.GONE));
        createButton.setOnClickListener(v -> createSchedule());
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> dismiss());
    }

    private void syncRepeatVisibility() {
        weeklyDaysLayout.setVisibility(repeatChoice.selected() == 2 ? View.VISIBLE : View.GONE);
    }

    private void showTimePicker() {
        // Keep the numeric clock in English; functional title and buttons follow the app language.
        android.content.res.Configuration numbers = new android.content.res.Configuration(getResources().getConfiguration());
        numbers.setLocale(java.util.Locale.US);
        androidx.appcompat.view.ContextThemeWrapper numberContext =
                new androidx.appcompat.view.ContextThemeWrapper(requireActivity(), R.style.Theme_ZenLock);
        numberContext.applyOverrideConfiguration(numbers);
        android.app.TimePickerDialog picker = new android.app.TimePickerDialog(numberContext,
                (view, hour, minute) -> {
                    selectedHour = hour;
                    selectedMinute = minute;
                    updateStartTimeDisplay();
                }, selectedHour, selectedMinute, false);
        picker.setTitle(getString(R.string.screen_starts_at));
        picker.setButton(android.content.DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), picker);
        picker.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                (dialog, which) -> dialog.dismiss());
        picker.show();
    }

    private void showDurationPicker() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_duration_picker, null);
        NumberPicker hoursPicker = dialogView.findViewById(R.id.hoursPicker);
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);

        hoursPicker.setMinValue(0);
        hoursPicker.setFormatter(value -> String.valueOf(value));
        hoursPicker.setMaxValue(23);
        hoursPicker.setValue(selectedDurationHours);

        String[] minuteLabels = new String[DURATION_MINUTE_STEPS.length];
        for (int i = 0; i < minuteLabels.length; i++) minuteLabels[i] = String.valueOf(DURATION_MINUTE_STEPS[i]);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(minuteLabels.length - 1);
        minutesPicker.setDisplayedValues(minuteLabels);
        minutesPicker.setValue(indexOf(DURATION_MINUTE_STEPS, selectedDurationMinutes, 1));

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(dialogView);
        com.grepguru.zenlock.ui.Popups.size(dialog);

        dialogView.findViewById(R.id.durationCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.durationSet).setOnClickListener(v -> {
            selectedDurationHours = hoursPicker.getValue();
            selectedDurationMinutes = DURATION_MINUTE_STEPS[minutesPicker.getValue()];
            updateDurationDisplay();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void updateStartTimeDisplay() {
        String amPm = new java.text.DateFormatSymbols(getResources().getConfiguration().getLocales().get(0)).getAmPmStrings()[selectedHour >= 12 ? 1 : 0];
        int displayHour = selectedHour % 12;
        if (displayHour == 0) displayHour = 12;
        startTimeText.setText(String.format(java.util.Locale.ROOT, getString(R.string.screen_clock_time), displayHour, selectedMinute, amPm));
    }

    private void updateDurationDisplay() {
        if (selectedDurationHours == 0 && selectedDurationMinutes == 0) {
            selectedDurationDisplay.setText(getString(R.string.screen_set_duration));
            return;
        }
        selectedDurationDisplay.setText(selectedDurationHours == 0
                ? String.format(java.util.Locale.ROOT, getString(R.string.screen_duration_minutes_short), selectedDurationMinutes)
                : selectedDurationMinutes == 0 ? String.format(java.util.Locale.ROOT, getString(R.string.screen_duration_hours_short), selectedDurationHours)
                : String.format(java.util.Locale.ROOT, getString(R.string.screen_duration_hours_minutes), selectedDurationHours, selectedDurationMinutes));
    }

    private void populate(ScheduleModel source) {
        nameInput.setText(source.getName());
        selectedHour = source.getStartHour();
        selectedMinute = source.getStartMinute();
        int totalMinutes = source.getFocusDurationMinutes();
        selectedDurationHours = totalMinutes / 60;
        selectedDurationMinutes = totalMinutes % 60;

        switch (source.getRepeatType()) {
            case ONCE: repeatChoice.select(0); break;
            case WEEKLY: repeatChoice.select(2); break;
            default: repeatChoice.select(1);
        }
        Set<Integer> repeatDays = source.getRepeatDays();
        for (int i = 0; i < dayChips.length; i++) dayChips[i].setChecked(repeatDays.contains(i + 1));

        preNotifySwitch.setChecked(source.isPreNotifyEnabled());
        notifyChoice.select(indexOf(NOTIFY_MINUTES, source.getPreNotifyMinutes(), 3));
    }

    private void createSchedule() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.screen_enter_a_schedule_name), Toast.LENGTH_SHORT).show();
            return;
        }
        int totalDurationMinutes = selectedDurationHours * 60 + selectedDurationMinutes;
        if (totalDurationMinutes == 0) {
            Toast.makeText(requireContext(), getString(R.string.screen_set_a_duration), Toast.LENGTH_SHORT).show();
            return;
        }

        ScheduleModel.RepeatType repeatType;
        switch (repeatChoice.selected()) {
            case 0: repeatType = ScheduleModel.RepeatType.ONCE; break;
            case 2: repeatType = ScheduleModel.RepeatType.WEEKLY; break;
            default: repeatType = ScheduleModel.RepeatType.DAILY;
        }

        if (repeatType == ScheduleModel.RepeatType.ONCE) {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.HOUR_OF_DAY, selectedHour);
            selected.set(Calendar.MINUTE, selectedMinute);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            if (selected.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), R.string.schedule_future_time, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Set<Integer> repeatDays = new HashSet<>();
        if (repeatType == ScheduleModel.RepeatType.WEEKLY) {
            for (int i = 0; i < dayChips.length; i++) {
                if (dayChips[i].isChecked()) repeatDays.add(i + 1);
            }
            if (repeatDays.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.screen_pick_at_least_one_day), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ScheduleModel schedule = scheduleToEdit != null ? scheduleToEdit : new ScheduleModel();
        schedule.setName(name);
        schedule.setStartHour(selectedHour);
        schedule.setStartMinute(selectedMinute);
        schedule.setFocusDurationMinutes(totalDurationMinutes);
        schedule.setRepeatType(repeatType);
        schedule.setRepeatDays(repeatDays);
        schedule.setPreNotifyEnabled(preNotifySwitch.isChecked());
        schedule.setPreNotifyMinutes(NOTIFY_MINUTES[notifyChoice.selected()]);

        if (listener != null) listener.onScheduleCreated(schedule);
        dismiss();
    }

    private static int indexOf(int[] values, int value, int fallback) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) return i;
        }
        return fallback;
    }

    private static final class ChipChoice {

        private final Chip[] chips;
        private int selected;
        private Runnable onChange;

        ChipChoice(Chip... chips) {
            this.chips = chips;
            for (int i = 0; i < chips.length; i++) {
                int index = i;
                chips[i].setOnClickListener(v -> select(index));
            }
        }

        void onChange(Runnable onChange) {
            this.onChange = onChange;
        }

        void select(int index) {
            selected = index;
            for (int i = 0; i < chips.length; i++) chips[i].setChecked(i == index);
            if (onChange != null) onChange.run();
        }

        int selected() {
            return selected;
        }
    }
}
