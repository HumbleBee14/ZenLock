package com.grepguru.zenlock.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import com.grepguru.zenlock.R;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={33,36})
public class ThemePreferenceTest {
    private final Context context=RuntimeEnvironment.getApplication();
    @After public void reset() {
        context.getSharedPreferences("FocusLockPrefs",0).edit().remove("appearance_mode").commit();
        ThemePreference.apply(context);
    }
    @Test public void followsPhoneByDefaultAndPersistsExplicitChoice() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,ThemePreference.nightMode(context));
        ThemePreference.select(context,1);
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO,ThemePreference.nightMode(context));
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.getDefaultNightMode());
        ThemePreference.select(context,2);
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES,ThemePreference.nightMode(context));
        ThemePreference.select(context,0);
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,AppCompatDelegate.getDefaultNightMode());
    }
    @Test public void bothPalettesHaveReadableTextAndMatchingSystemBarIcons() {
        for(int mode:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
            Configuration config=new Configuration(context.getResources().getConfiguration());
            config.uiMode=(config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)|mode;
            Context themed=context.createConfigurationContext(config);
            int surface=themed.getColor(R.color.surface);
            for(int text:new int[]{R.color.textPrimary,R.color.textSecondary,R.color.textTertiary,R.color.primaryLight,R.color.clockSelected}) {
                assertTrue("Text contrast in mode "+mode,ColorUtils.calculateContrast(themed.getColor(text),surface)>=4.5);
            }
            assertEquals(mode==Configuration.UI_MODE_NIGHT_NO,themed.getResources().getBoolean(R.bool.light_system_bars));
        }
    }
}
