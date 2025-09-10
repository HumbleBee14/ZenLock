package com.grepguru.zenlock.data.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Room type converters for custom data types
 * Handles conversion between Java objects and SQLite-compatible types
 */
public class Converters {
    
    /**
     * Convert timestamp to Date object
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    /**
     * Convert Date object to timestamp
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    /**
     * Convert boolean to integer for SQLite storage
     */
    @TypeConverter
    public static Integer booleanToInt(Boolean value) {
        return value == null ? null : (value ? 1 : 0);
    }
    
    /**
     * Convert integer to boolean from SQLite storage
     */
    @TypeConverter
    public static Boolean intToBoolean(Integer value) {
        return value == null ? null : (value == 1);
    }
}
