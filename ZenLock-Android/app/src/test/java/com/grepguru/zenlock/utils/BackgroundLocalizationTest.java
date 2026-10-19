package com.grepguru.zenlock.utils;

import static org.junit.Assert.*;
import android.content.Context;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.model.ScheduleModel;
import com.grepguru.zenlock.ui.AppLanguage;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 33})
public class BackgroundLocalizationTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @After public void resetLanguage() {
        AppLanguage.select(context, "en");
    }

    @Test public void existingModelsReadNewLanguageWithoutChangingStoredNames() {
        ScheduleModel schedule = new ScheduleModel();
        schedule.setName("My morning focus");
        schedule.setFocusDurationMinutes(90);
        schedule.setRepeatType(ScheduleModel.RepeatType.DAILY);
        AppLanguage.select(context, "de");
        assertEquals("Täglich", schedule.getRepeatDescription(context));
        assertEquals("1 Std. 30 Min.", schedule.getFormattedDuration(context));
        AppLanguage.select(context, "es");
        assertEquals("A diario", schedule.getRepeatDescription(context));
        assertEquals("1 h 30 min", schedule.getFormattedDuration(context));
        assertEquals("My morning focus", schedule.getName());
    }

    @Test public void analyticsDurationUsesAppLanguageFromApplicationContext() {
        AppLanguage.select(context, "de");
        SessionEntity session = new SessionEntity();
        session.actualDuration = 90 * 60_000L;
        assertEquals("1 Std. 30 Min.", session.getFormattedDuration(context));
        assertEquals("45 Sek.", DurationText.seconds(context, 45));
    }

    @Test public void backgroundNotificationUsesSingularAndPlural() {
        AppLanguage.select(context, "fr");
        Context localized = AppLanguage.localizedContext(context);
        assertEquals("Début dans 1 minute", String.format(java.util.Locale.ROOT, localized.getResources().getQuantityString(
                R.plurals.background_starts_in_minutes, 1), 1));
        assertEquals("Début dans 3 minutes", String.format(java.util.Locale.ROOT, localized.getResources().getQuantityString(
                R.plurals.background_starts_in_minutes, 3), 3));
    }
}
