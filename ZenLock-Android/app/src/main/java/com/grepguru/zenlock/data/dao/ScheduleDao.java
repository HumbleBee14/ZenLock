package com.grepguru.zenlock.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.grepguru.zenlock.data.entities.ScheduleEntity;

import java.util.List;

@Dao
public interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id ASC")
    List<ScheduleEntity> getAll();

    @Query("SELECT * FROM schedules WHERE enabled = 1 ORDER BY id ASC")
    List<ScheduleEntity> getEnabled();

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    ScheduleEntity getById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ScheduleEntity schedule);

    @Update
    int update(ScheduleEntity schedule);

    @Delete
    int delete(ScheduleEntity schedule);

    @Query("DELETE FROM schedules WHERE id = :id")
    int deleteById(int id);
}


