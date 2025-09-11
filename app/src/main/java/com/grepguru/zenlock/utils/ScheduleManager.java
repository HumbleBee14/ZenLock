package com.grepguru.zenlock.utils;

import android.content.Context;
import android.util.Log;

import com.grepguru.zenlock.data.dao.ScheduleDao;
import com.grepguru.zenlock.data.database.AnalyticsDatabase;
import com.grepguru.zenlock.data.entities.ScheduleEntity;
import com.grepguru.zenlock.model.ScheduleModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Schedule Manager - Handles schedule CRUD operations using Room
 * All schedule data is stored in SQLite database
 */
public class ScheduleManager {
    
    private static final String TAG = "ScheduleManager";
    
    private final Context context;
    private final ScheduleDao scheduleDao;
    
    public ScheduleManager(Context context) {
        this.context = context.getApplicationContext();
        AnalyticsDatabase db = AnalyticsDatabase.getDatabase(this.context);
        this.scheduleDao = db.scheduleDao();
    }
    
    // Mapping helpers
    private static ScheduleEntity toEntity(ScheduleModel m) {
        ScheduleEntity e = new ScheduleEntity();
        e.id = m.getId();
        e.name = m.getName() == null ? "" : m.getName();
        e.startHour = m.getStartHour();
        e.startMinute = m.getStartMinute();
        e.focusDurationMinutes = m.getFocusDurationMinutes();
        e.repeatType = m.getRepeatType() == null ? "DAILY" : m.getRepeatType().name();
        e.repeatDaysCsv = toCsv(m.getRepeatDays());
        e.preNotifyEnabled = m.isPreNotifyEnabled();
        e.preNotifyMinutes = m.getPreNotifyMinutes();
        e.enabled = m.isEnabled();
        return e;
    }
    
    private static ScheduleModel toModel(ScheduleEntity e) {
        ScheduleModel m = new ScheduleModel();
        m.setId(e.id);
        m.setName(e.name);
        m.setStartHour(e.startHour);
        m.setStartMinute(e.startMinute);
        m.setFocusDurationMinutes(e.focusDurationMinutes);
        try {
            m.setRepeatType(ScheduleModel.RepeatType.valueOf(e.repeatType));
        } catch (Exception ex) {
            m.setRepeatType(ScheduleModel.RepeatType.DAILY);
        }
        m.setRepeatDays(fromCsv(e.repeatDaysCsv));
        m.setPreNotifyEnabled(e.preNotifyEnabled);
        m.setPreNotifyMinutes(e.preNotifyMinutes);
        m.setEnabled(e.enabled);
        return m;
    }
    
    private static String toCsv(Set<Integer> set) {
        if (set == null || set.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Integer v : set) {
            if (v == null) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(v);
        }
        return sb.toString();
    }
    
    private static Set<Integer> fromCsv(String csv) {
        Set<Integer> set = new HashSet<>();
        if (csv == null || csv.isEmpty()) return set;
        String[] parts = csv.split(",");
        for (String p : parts) {
            try { set.add(Integer.parseInt(p.trim())); } catch (Exception ignored) {}
        }
        return set;
    }
    
    /** Create a new schedule */
    public ScheduleModel createSchedule(String name, int startHour, int startMinute,
                                        int focusDurationMinutes, ScheduleModel.RepeatType repeatType) {
        try {
            Log.d(TAG, "Creating schedule: " + name);
            ScheduleModel schedule = new ScheduleModel();
            schedule.setName(name);
            schedule.setStartHour(startHour);
            schedule.setStartMinute(startMinute);
            schedule.setFocusDurationMinutes(focusDurationMinutes);
            schedule.setRepeatType(repeatType);
            
            if (repeatType == ScheduleModel.RepeatType.WEEKLY) {
                schedule.getRepeatDays().add(Calendar.MONDAY);
                schedule.getRepeatDays().add(Calendar.TUESDAY);
                schedule.getRepeatDays().add(Calendar.WEDNESDAY);
                schedule.getRepeatDays().add(Calendar.THURSDAY);
                schedule.getRepeatDays().add(Calendar.FRIDAY);
            }
            
            ScheduleEntity entity = toEntity(schedule);
            entity.id = 0; // autogen
            Log.d(TAG, "Inserting entity: " + entity.name + " (repeatDays=" + entity.repeatDaysCsv + ")");
            long rowId = scheduleDao.insert(entity);
            schedule.setId((int) rowId);
            Log.d(TAG, "Created schedule: " + name + " (id=" + rowId + ")");
            return schedule;
        } catch (Exception e) {
            Log.e(TAG, "Error creating schedule: " + name, e);
            throw e;
        }
    }
    
    /** Update an existing schedule */
    public boolean updateSchedule(ScheduleModel schedule) {
        try {
            Log.d(TAG, "Updating schedule: " + schedule.getName() + " (id=" + schedule.getId() + ")");
            ScheduleEntity e = toEntity(schedule);
            Log.d(TAG, "Entity to update: " + e.name + " (repeatDays=" + e.repeatDaysCsv + ")");
            int rows = scheduleDao.update(e);
            Log.d(TAG, "Updated schedule: " + schedule.getName() + " rows=" + rows);
            return rows > 0;
        } catch (Exception ex) {
            Log.e(TAG, "Error updating schedule: " + schedule.getName(), ex);
            return false;
        }
    }
    
    /** Delete a schedule */
    public boolean deleteSchedule(int scheduleId) {
        int rows = scheduleDao.deleteById(scheduleId);
        Log.d(TAG, "Deleted schedule id=" + scheduleId + " rows=" + rows);
        return rows > 0;
    }
    
    /** Get all schedules */
    public List<ScheduleModel> getAllSchedules() {
        try {
            Log.d(TAG, "Getting all schedules from database...");
            List<ScheduleEntity> entities = scheduleDao.getAll();
            Log.d(TAG, "Found " + entities.size() + " entities in database");
            
            List<ScheduleModel> out = new ArrayList<>();
            for (ScheduleEntity e : entities) {
                Log.d(TAG, "Entity: id=" + e.id + ", name=" + e.name + ", enabled=" + e.enabled);
                ScheduleModel model = toModel(e);
                out.add(model);
            }
            Log.d(TAG, "Converted to " + out.size() + " models");
            return out;
        } catch (Exception e) {
            Log.e(TAG, "Error getting all schedules", e);
            return new ArrayList<>();
        }
    }
    
    /** Get enabled schedules only */
    public List<ScheduleModel> getEnabledSchedules() {
        List<ScheduleEntity> entities = scheduleDao.getEnabled();
        List<ScheduleModel> out = new ArrayList<>();
        for (ScheduleEntity e : entities) out.add(toModel(e));
        return out;
    }
    
    /** Get schedule by ID */
    public ScheduleModel getScheduleById(int scheduleId) {
        ScheduleEntity e = scheduleDao.getById(scheduleId);
        return e == null ? null : toModel(e);
    }
    
    /** Toggle schedule enabled/disabled */
    public boolean toggleSchedule(int scheduleId) {
        ScheduleEntity e = scheduleDao.getById(scheduleId);
        if (e == null) return false;
        e.enabled = !e.enabled;
        return scheduleDao.update(e) > 0;
    }
    
    /** Get schedules that should trigger today */
    public List<ScheduleModel> getSchedulesForToday() {
        List<ScheduleModel> todaySchedules = new ArrayList<>();
        Calendar today = Calendar.getInstance();
        int todayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        for (ScheduleModel schedule : getEnabledSchedules()) {
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
                    todaySchedules.add(schedule);
                    break;
            }
        }
        return todaySchedules;
    }
    
    /** Create quick template schedules */
    public void createQuickTemplates() {
        createSchedule("Morning Focus", 6, 0, 180, ScheduleModel.RepeatType.WEEKLY);
        createSchedule("Work Hours", 9, 0, 480, ScheduleModel.RepeatType.WEEKLY);
        createSchedule("Study Session", 19, 0, 180, ScheduleModel.RepeatType.DAILY);
        ScheduleModel weekendSchedule = createSchedule("Weekend Focus", 10, 0, 240, ScheduleModel.RepeatType.WEEKLY);
        weekendSchedule.getRepeatDays().clear();
        weekendSchedule.getRepeatDays().add(Calendar.SATURDAY);
        weekendSchedule.getRepeatDays().add(Calendar.SUNDAY);
        updateSchedule(weekendSchedule);
    }
    
    /** Check if quick templates exist */
    public boolean hasQuickTemplates() {
        for (ScheduleModel schedule : getAllSchedules()) {
            if (schedule.getName().equals("Morning Focus") ||
                schedule.getName().equals("Work Hours") ||
                schedule.getName().equals("Study Session") ||
                schedule.getName().equals("Weekend Focus")) {
                return true;
            }
        }
        return false;
    }
    
    /** Check if there are any enabled schedules */
    public boolean hasEnabledSchedules() {
        return !getEnabledSchedules().isEmpty();
    }
}