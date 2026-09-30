package com.grepguru.zenlock;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Run on an unlocked test device with AppBlockerService enabled. Uses real OS task navigation. */
@RunWith(AndroidJUnit4.class)
public class FocusEnforcementTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    @Test public void manualSessionReturnsFromHomeAndBlockedAppWithoutReplacingPinScreen() throws Exception {
        Context context = instrumentation.getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        // Do not suppress the service whose real accessibility callbacks this test exercises.
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        String enabled = android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String component = context.getPackageName() + "/" + AppBlockerService.class.getName();
        assertTrue("Enable ZenLock accessibility before running this test",
                enabled != null && enabled.contains(component));
        // Instrumentation restarts our process, so Android can mark its old service
        // connection crashed. Rebind the already-authorized service before testing.
        UiAutomation automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS);
        try {
            android.provider.Settings.Secure.putString(context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabled.replace(component, ""));
            SystemClock.sleep(250);
        } finally {
            android.provider.Settings.Secure.putString(context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabled);
            automation.dropShellPermissionIdentity();
        }
        long bindDeadline = SystemClock.uptimeMillis() + 5000;
        android.view.accessibility.AccessibilityManager accessibility =
                context.getSystemService(android.view.accessibility.AccessibilityManager.class);
        boolean bound;
        do {
            bound = false;
            for (android.accessibilityservice.AccessibilityServiceInfo info :
                    accessibility.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (component.equals(info.getId())) bound = true;
            }
            if (!bound) SystemClock.sleep(50);
        } while (!bound && SystemClock.uptimeMillis() < bindDeadline);
        assertTrue("Accessibility service did not reconnect", bound);
        prefs.edit().putBoolean("onboarding_seen", true).putBoolean("isLocked", false)
                .putBoolean("allow_launcher_during_lock", false).commit();
        Activity main = instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                prefs.edit().putBoolean("isLocked", true)
                        .putLong("lockEndTime", System.currentTimeMillis() + 900000)
                        .putLong("lockTargetDuration", 900000)
                        .putLong("uptimeAtLock", SystemClock.elapsedRealtime())
                        .putBoolean("wasDeviceRestarted", false)
                        .putString("current_session_source", "manual").commit();
                // Same activity launch as HomeFragment.startLock: no NEW_TASK flag.
                main.startActivity(new Intent(main, LockScreenActivity.class));
            });
            LockScreenActivity original = awaitLock();
            instrumentation.runOnMainSync(() -> ((android.widget.EditText)
                    original.findViewById(R.id.pinInput)).setText("12"));
            shell("input keyevent KEYCODE_HOME");
            SystemClock.sleep(700);
            assertSame("Home must return to the existing lock, not a rejected duplicate", original, awaitLock());
            shell("am start -a android.settings.SETTINGS");
            SystemClock.sleep(700);
            assertSame("A blocked app must return to the existing lock", original, awaitLock());
            instrumentation.runOnMainSync(() -> assertEquals("12", ((android.widget.EditText)
                    original.findViewById(R.id.pinInput)).getText().toString()));
            assertTrue(prefs.getBoolean("isLocked", false));
        } finally {
            prefs.edit().putBoolean("isLocked", false).remove("lockEndTime").commit();
            instrumentation.runOnMainSync(() -> {
                for (Stage stage : new Stage[]{Stage.RESUMED, Stage.STARTED, Stage.STOPPED, Stage.PAUSED}) {
                    for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(stage))) activity.finish();
                }
            });
        }
    }

    private LockScreenActivity awaitLock() {
        long deadline = SystemClock.uptimeMillis() + 5000;
        AtomicReference<LockScreenActivity> resumed = new AtomicReference<>();
        do {
            instrumentation.runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                    if (activity instanceof LockScreenActivity && !activity.isFinishing()) {
                        resumed.set((LockScreenActivity) activity);
                    }
                }
            });
            if (resumed.get() != null) return resumed.get();
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("No resumed lock screen after navigation while session is locked");
        return null;
    }


    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor fd = instrumentation.getUiAutomation(
                UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand(command);
             FileInputStream stream = new FileInputStream(fd.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) { }
        }
    }
}
