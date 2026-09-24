package com.grepguru.zenlock;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class LockScreenActivityTest {
    @Before
    public void prepareKeyguard() {
        // Robolectric's unlocked-keyguard shadow does not support a null dismiss callback.
        shadowOf(RuntimeEnvironment.getApplication().getSystemService(KeyguardManager.class))
                .setKeyguardLocked(true);
    }

    @After
    public void resetActiveScreen() {
        ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", false);
    }

    @Test
    public void rejectedDuplicateToleratesLateCallbacks() {
        ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", true);
        ActivityController<LockScreenActivity> controller =
                Robolectric.buildActivity(LockScreenActivity.class).create();
        LockScreenActivity activity = controller.get();
        assertTrue(activity.isFinishing());
        shadowOf(activity.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
        // Explicitly inject the unexpected resume seen in the production report.
        activity.onResume();
        activity.onPause();
        activity.onStop();
        controller.destroy();
        assertTrue("A rejected duplicate must not clear the live owner's state",
                LockScreenActivity.isActive());
    }

    @Test
    public void rejectedDuplicateStillInitializesPreferences() {
        ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", true);
        ActivityController<LockScreenActivity> controller =
                Robolectric.buildActivity(LockScreenActivity.class).create();
        assertNotNull(ReflectionHelpers.getField(controller.get(), "preferences"));
        controller.destroy();
        assertTrue(LockScreenActivity.isActive());
    }

    @Test
    public void activeLockSurvivesBackAndDuplicateDestruction() {
        SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("isLocked", true)
                .putLong("lockEndTime", System.currentTimeMillis() + 900_000)
                .putLong("lockTargetDuration", 900_000).commit();
        ActivityController<LockScreenActivity> owner =
                Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        assertFalse(owner.get().isFinishing());
        owner.get().getOnBackPressedDispatcher().onBackPressed();
        assertFalse(owner.get().isFinishing());
        assertTrue(prefs.getBoolean("isLocked", false));
        assertNotNull(shadowOf(owner.get().getSystemService(NotificationManager.class))
                .getNotification(1001));

        ActivityController<LockScreenActivity> duplicate =
                Robolectric.buildActivity(LockScreenActivity.class).create();
        duplicate.destroy();
        assertTrue(LockScreenActivity.isActive());
        assertTrue(prefs.getBoolean("isLocked", false));
        owner.pause().stop().destroy();
        assertFalse(LockScreenActivity.isActive());
        assertTrue("Destroying the UI must not unlock the session", prefs.getBoolean("isLocked", false));
    }
}
