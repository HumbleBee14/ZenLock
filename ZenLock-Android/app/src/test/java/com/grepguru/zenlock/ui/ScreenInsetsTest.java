package com.grepguru.zenlock.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {35, 36})
public class ScreenInsetsTest {
    @Test
    @Config(sdk = 28)
    @SuppressWarnings("deprecation")
    public void legacySystemBarsPreserveOriginalPadding() {
        View view = new View(RuntimeEnvironment.getApplication());
        view.setPadding(4, 8, 12, 16);
        ScreenInsets.apply(view);
        WindowInsetsCompat bars = new WindowInsetsCompat.Builder()
                .setSystemWindowInsets(Insets.of(0, 24, 0, 48))
                .setStableInsets(Insets.of(0, 24, 0, 48)).build();
        ViewCompat.dispatchApplyWindowInsets(view, bars);
        ViewCompat.dispatchApplyWindowInsets(view, bars);
        assertEquals(4, view.getPaddingLeft());
        assertEquals(32, view.getPaddingTop());
        assertEquals(12, view.getPaddingRight());
        assertEquals(64, view.getPaddingBottom());
    }

    @Test
    public void safePaddingTracksCutoutKeyboardAndNavigationWithoutAccumulating() {
        View view = new View(RuntimeEnvironment.getApplication());
        view.setPadding(4, 8, 12, 16);
        ScreenInsets.apply(view);
        WindowInsetsCompat bars = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(40, 0, 0, 0))
                .build();
        ViewCompat.dispatchApplyWindowInsets(view, bars);
        ViewCompat.dispatchApplyWindowInsets(view, bars);
        assertEquals(44, view.getPaddingLeft());
        assertEquals(32, view.getPaddingTop());
        assertEquals(12, view.getPaddingRight());
        assertEquals(64, view.getPaddingBottom());

        WindowInsetsCompat keyboard = new WindowInsetsCompat.Builder(bars)
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300)).build();
        ViewCompat.dispatchApplyWindowInsets(view, keyboard);
        assertEquals(316, view.getPaddingBottom());
        ViewCompat.dispatchApplyWindowInsets(view, bars);
        assertEquals(64, view.getPaddingBottom());
    }
}
