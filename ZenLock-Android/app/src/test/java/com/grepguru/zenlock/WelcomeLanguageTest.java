package com.grepguru.zenlock;

import static org.junit.Assert.*;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.viewpager2.widget.ViewPager2;
import com.grepguru.zenlock.ui.AppLanguage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 33})
public class WelcomeLanguageTest {
    @Test public void onboardingAlwaysStaysEnglishWithoutChangingTheSelectedAppLanguage() {
        Context context = RuntimeEnvironment.getApplication();
        for (String tag : AppLanguage.languageTags(context)) {
            AppLanguage.select(context, tag);
            org.robolectric.android.controller.ActivityController<WelcomeActivity> owner =
                    Robolectric.buildActivity(WelcomeActivity.class).setup();
            WelcomeActivity activity = owner.get();
            assertEquals("Next", ((TextView) activity.findViewById(R.id.welcomeNext)).getText());
            assertEquals(View.LAYOUT_DIRECTION_LTR, activity.findViewById(R.id.welcomeRoot).getLayoutDirection());
            assertEquals("Where attention goes", activity.getString(R.string.layout_where_attention_goes));
            assertEquals("Focus, your way", activity.getString(R.string.layout_focus_your_way));
            ((ViewPager2) activity.findViewById(R.id.welcomePager)).setCurrentItem(2, false);
            assertEquals("Get started", ((TextView) activity.findViewById(R.id.welcomeNext)).getText());
            assertEquals(tag, AppLanguage.selectedTag(activity));
            owner.pause().stop().destroy();
        }
    }
}
