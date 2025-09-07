package com.grepguru.zenlock.fragments;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

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
        // Hide coming soon banner since schedule functionality is now complete
        View comingSoonBanner = view.findViewById(R.id.comingSoonBanner);
        if (comingSoonBanner != null) {
            comingSoonBanner.setVisibility(View.GONE);
        }

        // Create schedule button
        createScheduleBtn = view.findViewById(R.id.createScheduleBtn);
        
        // RecyclerView
        schedulesRecyclerView = view.findViewById(R.id.schedulesRecyclerView);
        
        // Empty state
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        emptyStateText = view.findViewById(R.id.emptyStateText);
    }
    
    private void setupRecyclerView() {
        schedules = new ArrayList<>();
        scheduleAdapter = new ScheduleAdapter(schedules, new ScheduleAdapter.ScheduleListener() {
            @Override
            public void onToggleSchedule(ScheduleModel schedule) {
                boolean wasEnabled = schedule.isEnabled();
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
    
    private void setupCreateButton() {
        createScheduleBtn.setOnClickListener(v -> showCreateScheduleDialog());
    }
    
    private void loadSchedules() {
        Log.d(TAG, "Loading schedules...");
        schedules.clear();
        List<ScheduleModel> allSchedules = scheduleManager.getAllSchedules();
        Log.d(TAG, "Found " + allSchedules.size() + " schedules from manager");
        schedules.addAll(allSchedules);
        scheduleAdapter.notifyDataSetChanged();
        
        updateEmptyState();
        Log.d(TAG, "Schedules loaded: " + schedules.size());
    }
    
    private void updateEmptyState() {
        if (schedules.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            schedulesRecyclerView.setVisibility(View.GONE);
            emptyStateText.setText("No schedules created yet.\nTap 'Create Schedule' to get started!");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            schedulesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showCreateScheduleDialog() {
        CreateScheduleDialog dialog = new CreateScheduleDialog();
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
        loadSchedules(); // Refresh when returning to fragment
    }
}
