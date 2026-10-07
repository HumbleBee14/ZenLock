package com.grepguru.zenlock.admin;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.appcompat.widget.SwitchCompat;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.fragments.SettingsFragment;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33, 36})
public class UninstallProtectionSettingsTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle state) {
            setTheme(R.style.Theme_ZenLock);
            super.onCreate(state);
        }
    }

    private static androidx.fragment.app.DialogFragment confirmSheet(Host host) {
        shadowOf(Looper.getMainLooper()).idle();
        host.getSupportFragmentManager().executePendingTransactions();
        return (androidx.fragment.app.DialogFragment) host.getSupportFragmentManager().findFragmentByTag("ConfirmSheet");
    }

    @Test public void consentCancelAndExternalDeactivationKeepToggleHonest() {
        ActivityController<Host> owner = Robolectric.buildActivity(Host.class).create().start().resume();
        Host host = owner.get();
        host.getSharedPreferences("FocusLockPrefs", 0).edit().putBoolean("isLocked", false).commit();
        SettingsFragment fragment = new SettingsFragment();
        host.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        owner.visible();
        SwitchCompat toggle = fragment.requireView().findViewById(R.id.uninstallProtectionToggle);
        assertFalse(toggle.isChecked());
        toggle.performClick();
        BottomSheetDialog prompt = (BottomSheetDialog) ShadowDialog.getLatestDialog();
        assertTrue(prompt.isShowing());
        assertFalse("Requesting consent cannot optimistically enable protection", toggle.isChecked());
        prompt.findViewById(R.id.uninstallProtectionCancel).performClick();
        assertFalse(UninstallProtection.isEnabled(host));
        assertFalse(toggle.isChecked());
        DevicePolicyManager manager = host.getSystemService(DevicePolicyManager.class);
        shadowOf(manager).setActiveAdmin(UninstallProtection.component(host));
        owner.pause().resume();
        assertTrue(toggle.isChecked());
        toggle.performClick();
        assertTrue("Turning off must wait for confirmation", UninstallProtection.isEnabled(host));
        assertTrue(toggle.isChecked());
        androidx.fragment.app.DialogFragment warning = confirmSheet(host);
        assertNotNull(warning);
        warning.requireView().findViewById(R.id.confirmCancel).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600));
        assertTrue(UninstallProtection.isEnabled(host));
        assertTrue(toggle.isChecked());
        toggle.performClick();
        warning = confirmSheet(host);
        assertNotNull(warning);
        warning.requireView().findViewById(R.id.confirmAction).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600));
        assertFalse(UninstallProtection.isEnabled(host));
        assertFalse(toggle.isChecked());
        shadowOf(manager).setActiveAdmin(UninstallProtection.component(host));
        owner.pause().resume();
        assertTrue(toggle.isChecked());
        UninstallProtection.disable(host);
        owner.pause().resume();
        assertFalse(toggle.isChecked());
        owner.pause().stop().destroy();
    }
}
