package com.grepguru.zenlock.data;

import static org.junit.Assert.*;
import android.content.Context;
import com.grepguru.zenlock.MainActivity;
import com.grepguru.zenlock.data.database.AnalyticsDatabase;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.utils.AnalyticsManager;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public class AnalyticsRecoveryTest {
    @Test public void reopeningAfterTimerExpiredRecordsSessionAtDeadlineWithoutUsageAccess() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        AnalyticsDatabase db = AnalyticsDatabase.getDatabase(context);
        db.clearAllTables();
        long start = System.currentTimeMillis() - 180000;
        long end = start + 120000;
        context.getSharedPreferences("CurrentSessionPrefs",0).edit().clear()
                .putLong("session_start",start).putLong("session_target",60000)
                .putString("session_source","manual").commit();
        context.getSharedPreferences("FocusLockPrefs",0).edit().clear()
                .putBoolean("onboarding_seen",true).putBoolean("isLocked",true)
                .putLong("lockStartTime",start).putLong("lockEndTime",end)
                .putLong("lockTargetDuration",120000).commit();
        org.robolectric.android.controller.ActivityController<MainActivity> owner =
                Robolectric.buildActivity(MainActivity.class).create();
        List<SessionEntity> sessions = awaitSessions(db);
        assertEquals("An expired session must not disappear during startup cleanup",1,sessions.size());
        assertEquals(end,sessions.get(0).endTime);
        assertEquals(120000,sessions.get(0).actualDuration);
        assertEquals(120000,sessions.get(0).targetDuration);
        assertTrue(sessions.get(0).completed);
        assertFalse(context.getSharedPreferences("FocusLockPrefs",0).getBoolean("isLocked",true));
        owner.destroy();
    }

    @Test public void twoManagersEndingOneSessionDoNotDuplicateHistory() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        AnalyticsDatabase db = AnalyticsDatabase.getDatabase(context);
        db.clearAllTables();
        context.getSharedPreferences("CurrentSessionPrefs",0).edit().clear().commit();
        context.getSharedPreferences("FocusLockPrefs",0).edit().clear().commit();
        AnalyticsManager first = new AnalyticsManager(context);
        first.startSession(60000);
        AnalyticsManager second = new AnalyticsManager(context);
        first.endSession(false);
        second.endSession(false);
        awaitSessions(db);
        // Wait for both repository queues, not just the first inserted row.
        for (AnalyticsManager manager : new AnalyticsManager[]{first,second}) {
            Object repository = org.robolectric.util.ReflectionHelpers.getField(manager,"repository");
            java.util.concurrent.ExecutorService executor = org.robolectric.util.ReflectionHelpers.getField(repository,"executor");
            executor.submit(() -> {}).get(3, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(1,db.analyticsDao().getSessionsForDateRange(0,Long.MAX_VALUE).size());
    }
    private List<SessionEntity> awaitSessions(AnalyticsDatabase db) throws Exception {
        long deadline = System.nanoTime() + 2_000_000_000L;
        List<SessionEntity> rows;
        do {
            rows = db.analyticsDao().getSessionsForDateRange(0,Long.MAX_VALUE);
            if (!rows.isEmpty()) return rows;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        return rows;
    }
}
