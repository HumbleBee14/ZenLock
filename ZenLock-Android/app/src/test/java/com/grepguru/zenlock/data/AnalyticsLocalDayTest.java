package com.grepguru.zenlock.data;

import static org.junit.Assert.*;
import androidx.room.Room;
import com.grepguru.zenlock.data.database.AnalyticsDatabase;
import com.grepguru.zenlock.data.dao.AnalyticsDao;
import com.grepguru.zenlock.data.entities.SessionEntity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public class AnalyticsLocalDayTest {
    @Test public void sessionsBelongToLocalDayIncludingMidnightAndDstBoundaries() {
        TimeZone original = TimeZone.getDefault();
        AnalyticsDatabase db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AnalyticsDatabase.class)
                .allowMainThreadQueries().build();
        try {
            AnalyticsDao dao = db.analyticsDao();
            for (String zone : new String[]{"Asia/Kolkata", "America/Los_Angeles"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                LocalDate day = LocalDate.of(2026, 3, 8);
                long start = day.atStartOfDay(ZoneId.of(zone)).toInstant().toEpochMilli();
                long next = day.plusDays(1).atStartOfDay(ZoneId.of(zone)).toInstant().toEpochMilli();
                db.clearAllTables();
                insert(dao, start - 1, 1000);
                insert(dao, start, 60000);
                insert(dao, next - 1, 120000);
                insert(dao, next, 1000);
                assertEquals(zone, 2, dao.getSessionCountForDate("2026-03-08"));
                assertEquals(zone, Long.valueOf(180000), dao.getTotalFocusTimeForDate("2026-03-08"));
                assertEquals(zone, 2, dao.getCompletedSessionsForDate("2026-03-08"));
                assertEquals(180000, dao.calculateLocalDayStats("2026-03-08", start, next).totalFocusTime);
            }
        } finally { db.close(); TimeZone.setDefault(original); }
    }
    @Test public void sundayBelongsToPreviousMondayAcrossDst() {
        java.util.Calendar sunday = java.util.Calendar.getInstance(
                TimeZone.getTimeZone("America/Los_Angeles"), java.util.Locale.US);
        sunday.clear();
        sunday.set(2026, java.util.Calendar.MARCH, 8, 12, 0);
        long[] range = com.grepguru.zenlock.utils.MobileUsageTracker.getWeekTimestamps(sunday);
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        assertEquals(LocalDate.of(2026, 3, 2).atStartOfDay(zone).toInstant().toEpochMilli(), range[0]);
        assertEquals(LocalDate.of(2026, 3, 9).atStartOfDay(zone).toInstant().toEpochMilli() - 1, range[1]);
    }
    private void insert(AnalyticsDao dao, long start, long duration) {
        dao.insertSession(new SessionEntity(start, start, start+duration,duration,duration,true,"manual",100));
    }
}
