package com.grepguru.zenlock;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.grepguru.zenlock.ui.AppLanguage;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 33})
public class LocalizationLifecycleTest {
    @After public void reset() {
        Context context = RuntimeEnvironment.getApplication();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(android.app.LocaleManager.class).setApplicationLocales(LocaleList.getEmptyLocaleList());
        } else AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        org.robolectric.util.ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", false);
    }
    @Test public void focusSessionSurvivesFrenchConfigurationRecreation() {
        Context context = RuntimeEnvironment.getApplication();
        shadowOf(context.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", 0);
        long end = System.currentTimeMillis() + 900_000;
        prefs.edit().putBoolean("isLocked", true).putLong("lockEndTime", end)
                .putLong("lockTargetDuration", 900_000).commit();
        ActivityController<LockScreenActivity> owner = Robolectric.buildActivity(LockScreenActivity.class).setup();
        AppLanguage.select(owner.get(), "fr");
        Configuration config = new Configuration(owner.get().getResources().getConfiguration());
        config.setLocale(java.util.Locale.forLanguageTag("fr"));
        owner.configurationChange(config);
        assertFalse(owner.get().isFinishing());
        assertTrue(LockScreenActivity.isActive());
        assertTrue(prefs.getBoolean("isLocked", false));
        assertEquals(end, prefs.getLong("lockEndTime", 0));
        assertEquals("fr", owner.get().getResources().getConfiguration().getLocales().get(0).getLanguage());
        owner.pause().stop().destroy();
    }
}
