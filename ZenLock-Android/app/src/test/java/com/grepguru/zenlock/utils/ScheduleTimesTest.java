package com.grepguru.zenlock.utils;
import com.grepguru.zenlock.model.ScheduleModel;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScheduleTimesTest {
    private Calendar now() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear(); c.set(2026, Calendar.SEPTEMBER, 11, 9, 0, 0);
        return c;
    }
    private ScheduleModel model(ScheduleModel.RepeatType type) {
        ScheduleModel s = new ScheduleModel();
        s.setRepeatType(type); s.setStartHour(9); s.setStartMinute(0);
        return s;
    }
    @Test public void dailyAtExactTriggerAdvancesToTomorrow() {
        assertEquals(12, ScheduleTimes.next(model(ScheduleModel.RepeatType.DAILY), now()).get(Calendar.DAY_OF_MONTH));
    }
    @Test public void weeklyAtTriggerAdvancesToNextSelectedDay() {
        ScheduleModel s = model(ScheduleModel.RepeatType.WEEKLY);
        s.getRepeatDays().add(Calendar.FRIDAY);
        assertEquals(18, ScheduleTimes.next(s, now()).get(Calendar.DAY_OF_MONTH));
    }
    @Test public void oneTimePastOrCurrentMinuteHasNoFutureOccurrence() {
        assertNull(ScheduleTimes.next(model(ScheduleModel.RepeatType.ONCE), now()));
    }
    @Test public void futureOneTimeStaysTodayWithoutMutatingClock() {
        Calendar now = now(); ScheduleModel s = model(ScheduleModel.RepeatType.ONCE);
        s.setStartHour(10);
        assertEquals(11, ScheduleTimes.next(s, now).get(Calendar.DAY_OF_MONTH));
        assertEquals(9, now.get(Calendar.HOUR_OF_DAY));
    }
}
