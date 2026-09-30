package com.grepguru.zenlock.fragments;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Process;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.grepguru.zenlock.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class InsightsPermissionTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle state) { setTheme(R.style.Theme_ZenLock); super.onCreate(state); }
    }
    @Test public void missingAccessPromptsOncePerVisitAndRechecksAfterSettings() {
        ActivityController<Host> owner = Robolectric.buildActivity(Host.class).create().start().resume();
        Host host = owner.get();
        host.getSharedPreferences("FocusLockPrefs", 0).edit().putBoolean("isLocked", false).commit();
        AppOpsManager ops = host.getSystemService(AppOpsManager.class);
        shadowOf(ops).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), host.getPackageName(), AppOpsManager.MODE_IGNORED);
        AnalyticsFragment fragment = new AnalyticsFragment();
        host.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        Dialog prompt = ShadowDialog.getLatestDialog();
        assertNotNull("Missing usage access must be explained on entry", prompt);
        assertTrue(prompt.isShowing());
        ((AlertDialog) prompt).getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(android.os.Looper.getMainLooper()).idle();
        owner.pause().resume();
        assertFalse("Returning without permission must not immediately nag again", prompt.isShowing());
        shadowOf(ops).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), host.getPackageName(), AppOpsManager.MODE_ALLOWED);
        owner.pause().resume();
        assertEquals(android.view.View.GONE, fragment.requireView().findViewById(R.id.usagePermissionBanner).getVisibility());
        host.getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
        AnalyticsFragment granted = new AnalyticsFragment();
        host.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, granted).commitNow();
        assertSame("Granted access must not create another prompt", prompt, ShadowDialog.getLatestDialog());
        owner.pause().stop().destroy();
    }
}
