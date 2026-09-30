package com.grepguru.zenlock.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.grepguru.zenlock.data.entities.AppUsageEntity;
import com.grepguru.zenlock.data.entities.DailyMobileUsageEntity;
import com.grepguru.zenlock.data.entities.DailyStatsEntity;
import com.grepguru.zenlock.data.entities.MonthlyStatsEntity;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.data.entities.WeeklyStatsEntity;

import java.util.List;

/**
 * Data Access Object for Analytics database operations
 * Contains all SQL queries for managing focus session analytics data
 */
@Dao
public interface AnalyticsDao {

    static long[] localDayBounds(String date) {
        java.time.LocalDate day = java.time.LocalDate.parse(date);
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        return new long[]{day.atStartOfDay(zone).toInstant().toEpochMilli(),
                day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()};
    }

    String LOCAL_DAY_STATS = "SELECT :date AS date, COUNT(*) AS total_sessions, " +
            "COALESCE(SUM(actual_duration), 0) AS total_focus_time, " +
            "COALESCE(SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END), 0) AS completed_sessions, " +
            "COALESCE(SUM(CASE WHEN completed = 0 THEN 1 ELSE 0 END), 0) AS interrupted_sessions, " +
            "COALESCE(AVG(focus_score), 0) AS avg_focus_score, " +
            "COALESCE((SELECT SUM(usage_time) FROM app_usage WHERE is_whitelisted = 1 AND session_id IN " +
            "(SELECT session_id FROM sessions WHERE start_time >= :start AND start_time < :end)), 0) AS total_whitelisted_time, " +
            "0 AS created_at, 0 AS updated_at FROM sessions WHERE start_time >= :start AND start_time < :end";

    @Query(LOCAL_DAY_STATS)
    LiveData<DailyStatsEntity> observeLocalDayStats(String date, long start, long end);

    @Query(LOCAL_DAY_STATS)
    DailyStatsEntity calculateLocalDayStats(String date, long start, long end);

    
    // =====================================
    // SESSION OPERATIONS
    // =====================================
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSession(SessionEntity session);
    
    @Update
    void updateSession(SessionEntity session);
    
    @Query("SELECT * FROM sessions ORDER BY start_time DESC LIMIT :limit")
    LiveData<List<SessionEntity>> getRecentSessions(int limit);
    
    default LiveData<List<SessionEntity>> getSessionsForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getSessionsForLocalDay(bounds[0], bounds[1]);
    }

    @Query("SELECT * FROM sessions WHERE start_time >= :start AND start_time < :end ORDER BY start_time DESC")
    LiveData<List<SessionEntity>> getSessionsForLocalDay(long start, long end);
    
    @Query("SELECT * FROM sessions WHERE start_time >= :startTime AND start_time < :endTime ORDER BY start_time DESC")
    List<SessionEntity> getSessionsForDateRange(long startTime, long endTime);
    
    @Query("SELECT * FROM sessions WHERE session_id = :sessionId")
    LiveData<SessionEntity> getSessionById(long sessionId);
    
    // =====================================
    // APP USAGE OPERATIONS
    // =====================================
    
    @Insert
    void insertAppUsage(AppUsageEntity appUsage);
    
    @Insert
    void insertAppUsages(List<AppUsageEntity> appUsages);
    
    @Query("SELECT * FROM app_usage WHERE session_id = :sessionId ORDER BY usage_time DESC")
    LiveData<List<AppUsageEntity>> getAppUsageForSession(long sessionId);
    
    @Query("SELECT * FROM app_usage WHERE session_id = :sessionId ORDER BY usage_time DESC")
    List<AppUsageEntity> getAppUsageForSessionSync(long sessionId);
    
    // =====================================
    // DAILY STATS OPERATIONS
    // =====================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDailyStats(DailyStatsEntity dailyStats);
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    LiveData<DailyStatsEntity> getDailyStats(String date);
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    DailyStatsEntity getDailyStatsSync(String date);
    
    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    LiveData<List<DailyStatsEntity>> getDailyStatsForDateRange(String startDate, String endDate);
    
    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    List<DailyStatsEntity> getDailyStatsForDateRangeSync(String startDate, String endDate);
    
    // =====================================
    // WEEKLY STATS OPERATIONS
    // =====================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWeeklyStats(WeeklyStatsEntity weeklyStats);
    
    @Query("SELECT * FROM weekly_stats WHERE week_key = :weekKey")
    LiveData<WeeklyStatsEntity> getWeeklyStats(String weekKey);
    
    @Query("SELECT * FROM weekly_stats WHERE week_key = :weekKey")
    WeeklyStatsEntity getWeeklyStatsSync(String weekKey);
    
    @Query("SELECT * FROM weekly_stats ORDER BY week_key DESC LIMIT :limit")
    LiveData<List<WeeklyStatsEntity>> getRecentWeeklyStats(int limit);
    
    @Query("SELECT * FROM weekly_stats ORDER BY week_key DESC LIMIT :limit")
    List<WeeklyStatsEntity> getRecentWeeklyStatsSync(int limit);
    
    @Update
    void updateWeeklyStats(WeeklyStatsEntity weeklyStats);
    
    // =====================================
    // MONTHLY STATS OPERATIONS
    // =====================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMonthlyStats(MonthlyStatsEntity monthlyStats);
    
    @Query("SELECT * FROM monthly_stats WHERE month_key = :monthKey")
    LiveData<MonthlyStatsEntity> getMonthlyStats(String monthKey);
    
    @Query("SELECT * FROM monthly_stats WHERE month_key = :monthKey")
    MonthlyStatsEntity getMonthlyStatsSync(String monthKey);
    
    @Query("SELECT * FROM monthly_stats ORDER BY month_key DESC LIMIT :limit")
    LiveData<List<MonthlyStatsEntity>> getRecentMonthlyStats(int limit);
    
    // =====================================
    // ANALYTICS CALCULATIONS
    // =====================================
    
    @Query("SELECT COUNT(*) FROM sessions WHERE start_time >= :start AND start_time < :end")
    int getSessionCountForDateRange(long start, long end);

    default int getSessionCountForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getSessionCountForDateRange(bounds[0], bounds[1]);
    }
    
    @Query("SELECT SUM(actual_duration) FROM sessions WHERE start_time >= :start AND start_time < :end")
    Long getTotalFocusTimeForDateRange(long start, long end);

    default Long getTotalFocusTimeForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getTotalFocusTimeForDateRange(bounds[0], bounds[1]);
    }
    
    @Query("SELECT COUNT(*) FROM sessions WHERE start_time >= :start AND start_time < :end AND completed = 1")
    int getCompletedSessionsForDateRange(long start, long end);

    default int getCompletedSessionsForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getCompletedSessionsForDateRange(bounds[0], bounds[1]);
    }
    
    @Query("SELECT COUNT(*) FROM sessions WHERE start_time >= :start AND start_time < :end AND completed = 0")
    int getInterruptedSessionsForDateRange(long start, long end);

    default int getInterruptedSessionsForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getInterruptedSessionsForDateRange(bounds[0], bounds[1]);
    }
    
    @Query("SELECT AVG(focus_score) FROM sessions WHERE start_time >= :start AND start_time < :end")
    Float getAverageFocusScoreForDateRange(long start, long end);

    default Float getAverageFocusScoreForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getAverageFocusScoreForDateRange(bounds[0], bounds[1]);
    }
    
    @Query("SELECT SUM(usage_time) FROM app_usage WHERE session_id IN " +
           "(SELECT session_id FROM sessions WHERE start_time >= :start AND start_time < :end) " +
           "AND is_whitelisted = 1")
    Long getTotalWhitelistedTimeForDateRange(long start, long end);

    default Long getTotalWhitelistedTimeForDate(String date) {
        long[] bounds = localDayBounds(date);
        return getTotalWhitelistedTimeForDateRange(bounds[0], bounds[1]);
    }
    
    // Weekly calculations
    @Query("SELECT COUNT(*) FROM sessions WHERE strftime('%Y-%W', start_time/1000, 'unixepoch') = :weekKey")
    int getSessionCountForWeek(String weekKey);
    
    @Query("SELECT SUM(actual_duration) FROM sessions WHERE strftime('%Y-%W', start_time/1000, 'unixepoch') = :weekKey")
    Long getTotalFocusTimeForWeek(String weekKey);
    
    @Query("SELECT AVG(focus_score) FROM sessions WHERE strftime('%Y-%W', start_time/1000, 'unixepoch') = :weekKey")
    Float getAverageFocusScoreForWeek(String weekKey);
    
    @Query("SELECT (COUNT(CASE WHEN completed = 1 THEN 1 END) * 100.0 / COUNT(*)) " +
           "FROM sessions WHERE strftime('%Y-%W', start_time/1000, 'unixepoch') = :weekKey")
    Float getCompletionRateForWeek(String weekKey);
    
    // Monthly calculations
    @Query("SELECT COUNT(*) FROM sessions WHERE strftime('%Y-%m', start_time/1000, 'unixepoch') = :monthKey")
    int getSessionCountForMonth(String monthKey);
    
    @Query("SELECT SUM(actual_duration) FROM sessions WHERE strftime('%Y-%m', start_time/1000, 'unixepoch') = :monthKey")
    Long getTotalFocusTimeForMonth(String monthKey);
    
    @Query("SELECT COUNT(DISTINCT date(start_time/1000, 'unixepoch')) FROM sessions " +
           "WHERE strftime('%Y-%m', start_time/1000, 'unixepoch') = :monthKey")
    int getActiveDaysForMonth(String monthKey);
    
    // =====================================
    // COMPARISON QUERIES
    // =====================================
    
    @Query("SELECT * FROM daily_stats WHERE date = date('now', '-1 day')")
    DailyStatsEntity getYesterdayStats();
    
    @Query("SELECT * FROM weekly_stats WHERE week_key = strftime('%Y-W%W', 'now', '-7 days')")
    WeeklyStatsEntity getLastWeekStats();
    
    @Query("SELECT * FROM monthly_stats WHERE month_key = strftime('%Y-%m', 'now', '-1 month')")
    MonthlyStatsEntity getLastMonthStats();
    
    // =====================================
    // DATA CLEANUP OPERATIONS
    // =====================================
    
    @Query("DELETE FROM sessions WHERE start_time < :cutoffTime")
    void deleteOldSessions(long cutoffTime);
    
    @Query("DELETE FROM app_usage WHERE session_id IN " +
           "(SELECT session_id FROM sessions WHERE start_time < :cutoffTime)")
    void deleteOldAppUsage(long cutoffTime);
    
    @Query("DELETE FROM daily_stats WHERE date < :cutoffDate")
    void deleteOldDailyStats(String cutoffDate);
    
    @Query("DELETE FROM weekly_stats WHERE week_key < :cutoffWeekKey")
    void deleteOldWeeklyStats(String cutoffWeekKey);
    
    @Query("DELETE FROM monthly_stats WHERE month_key < :cutoffMonthKey")
    void deleteOldMonthlyStats(String cutoffMonthKey);
    
    // =====================================
    // BATCH OPERATIONS
    // =====================================
    
    @Transaction
    default void insertSessionWithAppUsage(SessionEntity session, List<AppUsageEntity> appUsages) {
        long sessionId = insertSession(session);
        if (sessionId == -1) return; // Already saved; do not duplicate its app usage.
        if (appUsages != null && !appUsages.isEmpty()) {
            for (AppUsageEntity appUsage : appUsages) {
                appUsage.sessionId = sessionId;
            }
            insertAppUsages(appUsages);
        }
    }
    
    @Transaction
    default void cleanupOldData(long sessionCutoffTime, String dailyCutoffDate, 
                               String weeklyCutoffKey, String monthlyCutoffKey) {
        deleteOldAppUsage(sessionCutoffTime);
        deleteOldSessions(sessionCutoffTime);
        deleteOldDailyStats(dailyCutoffDate);
        deleteOldWeeklyStats(weeklyCutoffKey);
        deleteOldMonthlyStats(monthlyCutoffKey);
    }
    
    // =====================================
    // UTILITY QUERIES
    // =====================================
    
    @Query("SELECT COUNT(*) FROM sessions")
    int getTotalSessionCount();
    
    @Query("SELECT SUM(actual_duration) FROM sessions")
    Long getTotalFocusTime();
    
    @Query("SELECT MAX(start_time) FROM sessions")
    Long getLastSessionTime();
    
    @Query("SELECT COUNT(DISTINCT date(start_time/1000, 'unixepoch')) FROM sessions")
    int getTotalActiveDays();
    
    @Query("SELECT SUM(actual_duration) FROM sessions WHERE start_time >= :startTime AND end_time <= :endTime")
    Long getTotalFocusTimeForPeriod(long startTime, long endTime);
    
    // =====================================
    // DAILY MOBILE USAGE OPERATIONS
    // =====================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDailyMobileUsage(DailyMobileUsageEntity dailyMobileUsage);
    
    @Update
    void updateDailyMobileUsage(DailyMobileUsageEntity dailyMobileUsage);
    
    @Query("SELECT * FROM daily_mobile_usage WHERE date = :date")
    DailyMobileUsageEntity getDailyMobileUsage(String date);
    
    @Query("SELECT * FROM daily_mobile_usage WHERE date = :date")
    LiveData<DailyMobileUsageEntity> getDailyMobileUsageLive(String date);
    
    @Query("SELECT * FROM daily_mobile_usage ORDER BY date DESC LIMIT :limit")
    List<DailyMobileUsageEntity> getRecentDailyMobileUsage(int limit);
    
    @Query("SELECT * FROM daily_mobile_usage WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    List<DailyMobileUsageEntity> getDailyMobileUsageRange(String startDate, String endDate);
    
    @Query("SELECT * FROM daily_mobile_usage ORDER BY date DESC")
    List<DailyMobileUsageEntity> getAllDailyMobileUsage();
    
    @Query("SELECT COUNT(*) FROM daily_mobile_usage")
    int getDailyMobileUsageCount();
    
    @Query("SELECT MIN(date) FROM daily_mobile_usage")
    String getOldestDailyMobileUsageDate();
    
    @Query("SELECT MAX(date) FROM daily_mobile_usage")
    String getNewestDailyMobileUsageDate();
    
    @Query("DELETE FROM daily_mobile_usage WHERE date = :date")
    void deleteDailyMobileUsage(String date);
    
    @Query("DELETE FROM daily_mobile_usage WHERE date < :cutoffDate")
    void deleteOldDailyMobileUsage(String cutoffDate);
    
    @Query("DELETE FROM daily_mobile_usage")
    void deleteAllDailyMobileUsage();
    
    // FIFO-like behavior: Keep only last 30 days
    @Transaction
    @Query("DELETE FROM daily_mobile_usage WHERE date < (SELECT date FROM daily_mobile_usage ORDER BY date DESC LIMIT 1 OFFSET 29)")
    void maintainMax30DaysMobileUsage();
}
