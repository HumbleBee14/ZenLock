package com.grepguru.focuslock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.grepguru.focuslock.model.ScheduleModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Schedule Manager - Handles schedule CRUD operations and storage
 * Uses structured SharedPreferences storage for easy migration in future
 */
public class ScheduleManager {
    
    private static final String TAG = "ScheduleManager";
    private static final String PREFS_NAME = "FocusLockSchedules";
    private static final String KEY_SCHEDULES = "schedules";
    private static final String KEY_NEXT_SCHEDULE_ID = "next_schedule_id";
    
    private Context context;
    private SharedPreferences preferences;
    private Gson gson;
    private List<ScheduleModel> schedules;
    private int nextScheduleId;
    
    public ScheduleManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.schedules = new ArrayList<>();
        this.nextScheduleId = 1;
        
        loadSchedules();
    }
    
    /**
     * Load schedules from SharedPreferences
     */
    private void loadSchedules() {
        try {
            String schedulesJson = preferences.getString(KEY_SCHEDULES, "[]");
            Type listType = new TypeToken<ArrayList<ScheduleModel>>(){}.getType();
            schedules = gson.fromJson(schedulesJson, listType);
            
            if (schedules == null) {
                schedules = new ArrayList<>();
            }
            
            nextScheduleId = preferences.getInt(KEY_NEXT_SCHEDULE_ID, 1);
            
            Log.d(TAG, "Loaded " + schedules.size() + " schedules");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load schedules", e);
            schedules = new ArrayList<>();
            nextScheduleId = 1;
        }
    }
    
    /**
     * Save schedules to SharedPreferences
     */
    private void saveSchedules() {
        try {
            String schedulesJson = gson.toJson(schedules);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_SCHEDULES, schedulesJson);
            editor.putInt(KEY_NEXT_SCHEDULE_ID, nextScheduleId);
            editor.apply();
            
            Log.d(TAG, "Saved " + schedules.size() + " schedules");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save schedules", e);
        }
    }
    
    /**
     * Create a new schedule
     */
    public ScheduleModel createSchedule(String name, int startHour, int startMinute, 
                                      int focusDurationMinutes, ScheduleModel.RepeatType repeatType) {
        ScheduleModel schedule = new ScheduleModel();
        schedule.setId(nextScheduleId++);
        schedule.setName(name);
        schedule.setStartHour(startHour);
        schedule.setStartMinute(startMinute);
        schedule.setFocusDurationMinutes(focusDurationMinutes);
        schedule.setRepeatType(repeatType);
        
        // Set default repeat days for weekly
        if (repeatType == ScheduleModel.RepeatType.WEEKLY) {
            schedule.getRepeatDays().add(Calendar.MONDAY);
            schedule.getRepeatDays().add(Calendar.TUESDAY);
            schedule.getRepeatDays().add(Calendar.WEDNESDAY);
            schedule.getRepeatDays().add(Calendar.THURSDAY);
            schedule.getRepeatDays().add(Calendar.FRIDAY);
        }
        
        schedules.add(schedule);
        saveSchedules();
        
        Log.d(TAG, "Created schedule: " + name);
        return schedule;
    }
    
    /**
     * Update an existing schedule
     */
    public boolean updateSchedule(ScheduleModel schedule) {
        for (int i = 0; i < schedules.size(); i++) {
            if (schedules.get(i).getId() == schedule.getId()) {
                schedules.set(i, schedule);
                saveSchedules();
                Log.d(TAG, "Updated schedule: " + schedule.getName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Delete a schedule
     */
    public boolean deleteSchedule(int scheduleId) {
        for (int i = 0; i < schedules.size(); i++) {
            if (schedules.get(i).getId() == scheduleId) {
                ScheduleModel removed = schedules.remove(i);
                saveSchedules();
                Log.d(TAG, "Deleted schedule: " + removed.getName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all schedules
     */
    public List<ScheduleModel> getAllSchedules() {
        return new ArrayList<>(schedules);
    }
    
    /**
     * Get enabled schedules only
     */
    public List<ScheduleModel> getEnabledSchedules() {
        List<ScheduleModel> enabledSchedules = new ArrayList<>();
        for (ScheduleModel schedule : schedules) {
            if (schedule.isEnabled()) {
                enabledSchedules.add(schedule);
            }
        }
        return enabledSchedules;
    }
    
    /**
     * Get schedule by ID
     */
    public ScheduleModel getScheduleById(int scheduleId) {
        for (ScheduleModel schedule : schedules) {
            if (schedule.getId() == scheduleId) {
                return schedule;
            }
        }
        return null;
    }
    
    /**
     * Toggle schedule enabled/disabled
     */
    public boolean toggleSchedule(int scheduleId) {
        ScheduleModel schedule = getScheduleById(scheduleId);
        if (schedule != null) {
            schedule.setEnabled(!schedule.isEnabled());
            saveSchedules();
            Log.d(TAG, "Toggled schedule: " + schedule.getName() + " to " + schedule.isEnabled());
            return true;
        }
        return false;
    }
    
    /**
     * Get schedules that should trigger today
     */
    public List<ScheduleModel> getSchedulesForToday() {
        List<ScheduleModel> todaySchedules = new ArrayList<>();
        Calendar today = Calendar.getInstance();
        int todayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        
        for (ScheduleModel schedule : schedules) {
            if (!schedule.isEnabled()) continue;
            
            switch (schedule.getRepeatType()) {
                case DAILY:
                    todaySchedules.add(schedule);
                    break;
                case WEEKLY:
                    if (schedule.getRepeatDays().contains(todayOfWeek)) {
                        todaySchedules.add(schedule);
                    }
                    break;
                case ONCE:
                    // For once, we'll need to check the specific date
                    // This is simplified for now
                    todaySchedules.add(schedule);
                    break;
            }
        }
        
        return todaySchedules;
    }
    
    /**
     * Create quick template schedules
     */
    public void createQuickTemplates() {
        // Morning Focus (6 AM - 9 AM)
        createSchedule("Morning Focus", 6, 0, 180, ScheduleModel.RepeatType.WEEKLY);
        
        // Work Hours (9 AM - 5 PM)
        createSchedule("Work Hours", 9, 0, 480, ScheduleModel.RepeatType.WEEKLY);
        
        // Study Session (7 PM - 10 PM)
        createSchedule("Study Session", 19, 0, 180, ScheduleModel.RepeatType.DAILY);
        
        // Weekend Focus (10 AM - 2 PM)
        ScheduleModel weekendSchedule = createSchedule("Weekend Focus", 10, 0, 240, ScheduleModel.RepeatType.WEEKLY);
        weekendSchedule.getRepeatDays().clear();
        weekendSchedule.getRepeatDays().add(Calendar.SATURDAY);
        weekendSchedule.getRepeatDays().add(Calendar.SUNDAY);
        updateSchedule(weekendSchedule);
    }
    
    /**
     * Check if quick templates exist
     */
    public boolean hasQuickTemplates() {
        for (ScheduleModel schedule : schedules) {
            if (schedule.getName().equals("Morning Focus") || 
                schedule.getName().equals("Work Hours") ||
                schedule.getName().equals("Study Session") ||
                schedule.getName().equals("Weekend Focus")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if there are any enabled schedules
     */
    public boolean hasEnabledSchedules() {
        for (ScheduleModel schedule : schedules) {
            if (schedule.isEnabled()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Export schedules as JSON (for future sync/migration)
     */
    public String exportSchedules() {
        return gson.toJson(schedules);
    }
    
    /**
     * Import schedules from JSON (for future sync/migration)
     */
    public boolean importSchedules(String jsonData) {
        try {
            Type listType = new TypeToken<ArrayList<ScheduleModel>>(){}.getType();
            List<ScheduleModel> importedSchedules = gson.fromJson(jsonData, listType);
            
            if (importedSchedules != null) {
                schedules = importedSchedules;
                saveSchedules();
                Log.d(TAG, "Imported " + schedules.size() + " schedules");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to import schedules", e);
        }
        return false;
    }
} 