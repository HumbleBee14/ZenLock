package com.grepguru.zenlock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.model.ScheduleModel;
import com.grepguru.zenlock.utils.ScheduleManager;
import com.grepguru.zenlock.utils.ScheduleActivator;
import com.grepguru.zenlock.ui.adapter.ScheduleAdapter;
import com.grepguru.zenlock.CreateScheduleDialog;
import com.grepguru.zenlock.guards.UnlockMethodGuard;
import com.grepguru.zenlock.permissions.FeaturePermissions;
import com.grepguru.zenlock.permissions.PermissionGate;

import com.grepguru.zenlock.utils.ScheduleTimes;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Schedule Fragment - Manages zen lock schedules
 * Features: Create, edit, delete, and view schedules
 */
public class ScheduleFragment extends Fragment {
    
    private static final String TAG = "ScheduleFragment";
    
    private ScheduleManager scheduleManager;
    private ScheduleActivator scheduleActivator;
    private RecyclerView schedulesRecyclerView;
    private ScheduleAdapter scheduleAdapter;
    private List<ScheduleModel> schedules;
    
    // Create schedule button
    private Button createScheduleBtn;
    
    // Empty state
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private LinearLayout templateList;
    private TextView upNextName;
    private TextView upNextTime;
    private TextView upNextCountdown;
    private LinearLayout weekStrip;
    private final android.os.Handler ticker = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tick = this::refreshOverview;
    private static final String[] DAY_LETTERS = {"S", "M", "T", "W", "T", "F", "S"};
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        
        // Initialize managers
        scheduleManager = new ScheduleManager(requireContext());
        scheduleActivator = new ScheduleActivator(requireContext());
        
        // Initialize views
        initializeViews(view);
        setupRecyclerView();
        setupCreateButton();
        
        // Load schedules
        loadSchedules();
        
        return view;
    }
    
    private void initializeViews(View view) {
        // Create schedule button
        createScheduleBtn = view.findViewById(R.id.createScheduleBtn);
        
        // RecyclerView
        schedulesRecyclerView = view.findViewById(R.id.schedulesRecyclerView);
        
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        templateList = view.findViewById(R.id.templateList);
        upNextName = view.findViewById(R.id.upNextName);
        upNextTime = view.findViewById(R.id.upNextTime);
        upNextCountdown = view.findViewById(R.id.upNextCountdown);
        weekStrip = view.findViewById(R.id.weekStrip);
        buildWeekStrip();
        buildTemplates();
    }

    private void buildWeekStrip() {
        weekStrip.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int day = Calendar.MONDAY; day <= Calendar.MONDAY + 6; day++) {
            int dayOfWeek = ((day - 1) % 7) + 1;
            View item = inflater.inflate(R.layout.item_week_day, weekStrip, false);
            ((TextView) item.findViewById(R.id.dayLabel)).setText(DAY_LETTERS[dayOfWeek - 1]);
            item.setTag(dayOfWeek);
            weekStrip.addView(item);
        }
    }

    private void buildTemplates() {
        templateList.removeAllViews();
        Set<Integer> weekdays = new HashSet<>(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY));
        ScheduleModel[] templates = {
                template("Morning deep work", 9, 0, 120, ScheduleModel.RepeatType.WEEKLY, weekdays),
                template("Lunch break", 13, 0, 45, ScheduleModel.RepeatType.WEEKLY, weekdays),
                template("Wind down", 22, 0, 60, ScheduleModel.RepeatType.DAILY, new HashSet<>())};
        for (ScheduleModel model : templates) {
            Chip chip = new Chip(requireContext(), null, R.style.ZenChip);
            chip.setChipBackgroundColorResource(R.color.backgroundTertiary);
            chip.setChipStrokeWidth(0f);
            chip.setTextColor(requireContext().getColor(R.color.textPrimary));
            chip.setCheckable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setText(model.getName() + "  ·  " + model.getFormattedStartTime() + "  ·  " + model.getFormattedDuration());
            chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
            params.bottomMargin = dp(8);
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> showCreateScheduleDialog(model));
            templateList.addView(chip);
        }
    }

    private ScheduleModel template(String name, int hour, int minute, int duration, ScheduleModel.RepeatType repeat, Set<Integer> days) {
        ScheduleModel model = new ScheduleModel();
        model.setName(name);
        model.setStartHour(hour);
        model.setStartMinute(minute);
        model.setFocusDurationMinutes(duration);
        model.setRepeatType(repeat);
        model.setRepeatDays(days);
        return model;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshOverview() {
        ticker.removeCallbacks(tick);
        if (!isAdded()) return;
        Calendar now = Calendar.getInstance();
        ScheduleModel nextSchedule = null;
        Calendar nextTime = null;
        Set<Integer> activeDays = new HashSet<>();
        for (ScheduleModel schedule : schedules) {
            if (!schedule.isEnabled()) continue;
            Calendar trigger = ScheduleTimes.next(schedule, now);
            if (trigger == null) continue;
            if (nextTime == null || trigger.before(nextTime)) {
                nextTime = trigger;
                nextSchedule = schedule;
            }
            switch (schedule.getRepeatType()) {
                case DAILY: for (int d = 1; d <= 7; d++) activeDays.add(d); break;
                case WEEKLY: activeDays.addAll(schedule.getRepeatDays()); break;
                default: activeDays.add(trigger.get(Calendar.DAY_OF_WEEK));
            }
        }

        if (nextSchedule == null) {
            upNextName.setText("Nothing scheduled");
            upNextCountdown.setVisibility(View.GONE);
            upNextTime.setVisibility(View.GONE);
        } else {
            upNextName.setText(nextSchedule.getName());
            upNextCountdown.setVisibility(View.VISIBLE);
            upNextTime.setVisibility(View.VISIBLE);
            upNextCountdown.setText(countdown(nextTime.getTimeInMillis() - now.getTimeInMillis()));
            upNextTime.setText(dayLabel(now, nextTime) + " · " + nextSchedule.getFormattedStartTime() + " · " + nextSchedule.getFormattedDuration());
            long untilNextMinute = 60000 - (now.getTimeInMillis() % 60000);
            ticker.postDelayed(tick, untilNextMinute + 50);
        }

        int today = now.get(Calendar.DAY_OF_WEEK);
        for (int i = 0; i < weekStrip.getChildCount(); i++) {
            View item = weekStrip.getChildAt(i);
            int dayOfWeek = (Integer) item.getTag();
            TextView label = item.findViewById(R.id.dayLabel);
            label.setTextColor(requireContext().getColor(dayOfWeek == today ? R.color.textPrimary : R.color.textTertiary));
            item.findViewById(R.id.dayDot).setVisibility(activeDays.contains(dayOfWeek) ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static String countdown(long millis) {
        long minutes = Math.max(0, millis / 60000);
        if (minutes < 60) return "in " + Math.max(1, minutes) + "m";
        long hours = minutes / 60;
        if (hours < 24) return "in " + hours + "h " + (minutes % 60) + "m";
        long days = hours / 24;
        return "in " + days + "d " + (hours % 24) + "h";
    }

    private static String dayLabel(Calendar now, Calendar target) {
        Calendar tomorrow = (Calendar) now.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        if (sameDay(now, target)) return "Today";
        if (sameDay(tomorrow, target)) return "Tomorrow";
        return new java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(target.getTime());
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
    
    private void setupRecyclerView() {
        schedules = new ArrayList<>();
        scheduleAdapter = new ScheduleAdapter(schedules, new ScheduleAdapter.ScheduleListener() {
            @Override
            public void onToggleSchedule(ScheduleModel schedule) {
                if (schedule.isEnabled()) {
                    toggleSchedule(schedule);
                } else {
                    UnlockMethodGuard.ensure(requireActivity(), "Enable anyway", () ->
                            PermissionGate.ensure(requireActivity(), FeaturePermissions.schedule(requireContext()), () -> {
                                if (isAdded()) toggleSchedule(schedule);
                            }));
                }
            }

            @Override
            public void onEditSchedule(ScheduleModel schedule) {
                showEditScheduleDialog(schedule);
            }

            @Override
            public void onDeleteSchedule(ScheduleModel schedule) {
                showDeleteConfirmation(schedule);
            }
        });

        schedulesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        schedulesRecyclerView.setAdapter(scheduleAdapter);
    }

    private void toggleSchedule(ScheduleModel schedule) {
        scheduleManager.toggleSchedule(schedule.getId());
        
        // Get updated schedule
        ScheduleModel updatedSchedule = scheduleManager.getScheduleById(schedule.getId());
        if (updatedSchedule != null) {
            if (updatedSchedule.isEnabled()) {
                // Schedule was enabled, activate it
                scheduleActivator.scheduleSchedule(updatedSchedule);
                Toast.makeText(requireContext(), "Schedule activated: " + updatedSchedule.getName(), Toast.LENGTH_SHORT).show();
            } else {
                // Schedule was disabled, cancel it
                scheduleActivator.cancelSchedule(updatedSchedule);
                Toast.makeText(requireContext(), "Schedule deactivated: " + updatedSchedule.getName(), Toast.LENGTH_SHORT).show();
            }
        }
        
        loadSchedules();
    }
    
    private void setupCreateButton() {
        createScheduleBtn.setOnClickListener(v -> showCreateScheduleDialog());
    }
    
    private void loadSchedules() {
        Log.d(TAG, "Loading schedules...");
        try {
            schedules.clear();
            List<ScheduleModel> allSchedules = scheduleManager.getAllSchedules();
            Log.d(TAG, "Found " + allSchedules.size() + " schedules from manager");
            
            // Debug: Log each schedule
            for (ScheduleModel schedule : allSchedules) {
                Log.d(TAG, "Schedule: " + schedule.getName() + " (id=" + schedule.getId() + ", enabled=" + schedule.isEnabled() + ")");
            }
            
            schedules.addAll(allSchedules);
            scheduleAdapter.notifyDataSetChanged();
            
            updateEmptyState();
            refreshOverview();
        } catch (Exception e) {
            Log.e(TAG, "Error loading schedules", e);
            Toast.makeText(requireContext(), "Error loading schedules: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateEmptyState() {
        if (schedules.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            schedulesRecyclerView.setVisibility(View.GONE);
            emptyStateText.setText("No schedules yet");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            schedulesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showCreateScheduleDialog() {
        showCreateScheduleDialog(null);
    }

    private void showCreateScheduleDialog(ScheduleModel template) {
        UnlockMethodGuard.ensure(requireActivity(), "Create anyway", () ->
                PermissionGate.ensure(requireActivity(), FeaturePermissions.schedule(requireContext()), () -> {
                    if (isAdded()) openCreateScheduleDialog(template);
                }));
    }

    private void openCreateScheduleDialog(ScheduleModel template) {
        CreateScheduleDialog dialog = new CreateScheduleDialog();
        dialog.setTemplate(template);
        dialog.setScheduleListener(new CreateScheduleDialog.ScheduleListener() {
            @Override
            public void onScheduleCreated(ScheduleModel schedule) {
                Log.d(TAG, "Schedule creation callback received for: " + schedule.getName());
                
                // Create the schedule using ScheduleManager
                ScheduleModel newSchedule = scheduleManager.createSchedule(
                    schedule.getName(),
                    schedule.getStartHour(),
                    schedule.getStartMinute(),
                    schedule.getFocusDurationMinutes(),
                    schedule.getRepeatType()
                );
                
                // Copy additional properties
                newSchedule.setRepeatDays(schedule.getRepeatDays());
                newSchedule.setPreNotifyEnabled(schedule.isPreNotifyEnabled());
                newSchedule.setPreNotifyMinutes(schedule.getPreNotifyMinutes());
                
                // Save the updated schedule
                scheduleManager.updateSchedule(newSchedule);
                
                // Reload and display schedules
                loadSchedules();
                
                // Activate the schedule if enabled
                if (newSchedule.isEnabled()) {
                    scheduleActivator.scheduleSchedule(newSchedule);
                    Toast.makeText(requireContext(), "Schedule created and activated: " + newSchedule.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Schedule created: " + newSchedule.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.show(getChildFragmentManager(), "CreateSchedule");
    }
    
    private void showEditScheduleDialog(ScheduleModel schedule) {
        CreateScheduleDialog dialog = new CreateScheduleDialog();
        dialog.setScheduleToEdit(schedule);
        dialog.setScheduleListener(new CreateScheduleDialog.ScheduleListener() {
            @Override
            public void onScheduleCreated(ScheduleModel updatedSchedule) {
                scheduleManager.updateSchedule(updatedSchedule);
                loadSchedules();
                
                // Reactivate the updated schedule if enabled
                if (updatedSchedule.isEnabled()) {
                    scheduleActivator.scheduleSchedule(updatedSchedule);
                    Toast.makeText(requireContext(), "Schedule updated and activated: " + updatedSchedule.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    scheduleActivator.cancelSchedule(updatedSchedule);
                    Toast.makeText(requireContext(), "Schedule updated: " + updatedSchedule.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.show(getChildFragmentManager(), "EditSchedule");
    }
    
    private void showDeleteConfirmation(ScheduleModel schedule) {
        // Cancel the schedule activation first
        scheduleActivator.cancelSchedule(schedule);
        
        scheduleManager.deleteSchedule(schedule.getId());
        loadSchedules();
        Toast.makeText(requireContext(), "Schedule deleted: " + schedule.getName(), Toast.LENGTH_SHORT).show();
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

        loadSchedules();
    }

    @Override
    public void onPause() {
        super.onPause();
        ticker.removeCallbacks(tick);
    }

    
    
}
