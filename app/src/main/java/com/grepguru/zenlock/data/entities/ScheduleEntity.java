package com.grepguru.zenlock.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a saved schedule.
 * Repeat days are stored as a comma-separated list of integers (Calendar.DAY_OF_WEEK values).
 */
@Entity(tableName = "schedules")
public class ScheduleEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    public int startHour;
    public int startMinute;
    public int focusDurationMinutes;

    /**
     * Repeat type values: ONCE, DAILY, WEEKLY
     */
    @NonNull
    public String repeatType;

    /**
     * Comma-separated Calendar.DAY_OF_WEEK ints, e.g. "1,2,3". Empty string if none.
     */
    @NonNull
    public String repeatDaysCsv;

    public boolean preNotifyEnabled;
    public int preNotifyMinutes;

    public boolean enabled;
}


