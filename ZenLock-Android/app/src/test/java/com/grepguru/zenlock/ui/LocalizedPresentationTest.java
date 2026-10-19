package com.grepguru.zenlock.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.utils.OTPManager;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 33})
public class LocalizedPresentationTest {
    public static class PickerHost extends androidx.appcompat.app.AppCompatActivity {
        @Override protected void onCreate(android.os.Bundle state) {
            setTheme(R.style.Theme_ZenLock);
            super.onCreate(state);
            setContentView(new android.widget.FrameLayout(this));
        }
    }

    @Test @Config(qualifiers = "ar")
    public void clockUsesZeroToNineWhileItsActionsFollowAppLanguage() {
        org.robolectric.android.controller.ActivityController<PickerHost> host =
                org.robolectric.Robolectric.buildActivity(PickerHost.class).setup();
        com.grepguru.zenlock.CreateScheduleDialog sheet = new com.grepguru.zenlock.CreateScheduleDialog();
        sheet.show(host.get().getSupportFragmentManager(), "schedule");
        host.get().getSupportFragmentManager().executePendingTransactions();
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(sheet, "showTimePicker");
        android.app.AlertDialog latest = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(latest instanceof android.app.TimePickerDialog);
        android.app.TimePickerDialog picker = (android.app.TimePickerDialog) latest;
        assertEquals("en", picker.getContext().getResources().getConfiguration().getLocales().get(0).getLanguage());
        assertSame("Numeric dialog must retain the Activity window token",
                host.get().getSystemService(Context.WINDOW_SERVICE),
                picker.getContext().getSystemService(Context.WINDOW_SERVICE));
        assertEquals(sheet.getString(android.R.string.ok), picker.getButton(android.content.DialogInterface.BUTTON_POSITIVE).getText());
        picker.updateTime(15, 37);
        picker.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
        assertEquals(15, (int) org.robolectric.util.ReflectionHelpers.getField(sheet, "selectedHour"));
        assertEquals(37, (int) org.robolectric.util.ReflectionHelpers.getField(sheet, "selectedMinute"));
        sheet.dismiss();
        host.pause().stop().destroy();
    }

    private Context context(String tag) {
        Context application = RuntimeEnvironment.getApplication();
        Configuration config = new Configuration(application.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(tag));
        return application.createConfigurationContext(config);
    }

    @Test public void everyLanguageInterpolatesValidityAndPreservesAsciiOtp() {
        for (String tag : AppLanguage.languageTags(context("en"))) {
            Context localized = context(tag);
            String expectedMinutes = "7";
            assertTrue(tag, String.format(Locale.ROOT, localized.getString(R.string.partner_sms_validity_note), 7).contains(expectedMinutes));
            assertTrue(tag, String.format(Locale.ROOT, localized.getString(R.string.background_valid_for_minutes), 7).contains(expectedMinutes));
            String sms = String.format(Locale.ROOT, localized.getString(R.string.background_zenlock_alert_unlock_code_1_s_your_accountability_partner_wants_to_end_thei),
                    "1234", 7);
            assertTrue(tag, sms.contains("1234"));
            assertTrue(tag, sms.contains(expectedMinutes));
            assertFalse(tag, sms.matches("(?s).*[\\u0660-\\u0669\\u06f0-\\u06f9].*"));
        }
    }

    @Test public void displayedValidityMatchesActualStoredOtpLifetime() {
        Context context = context("en");
        OTPManager manager = new OTPManager(context);
        manager.generateOTP();
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        assertEquals(OTPManager.validityMinutes() * 60_000L,
                prefs.getLong("otp_expiry", 0) - prefs.getLong("otp_generation_time", 0));
        assertEquals("SMS charges apply. Codes stay valid for 5 minutes.",
                String.format(Locale.ROOT, context.getString(R.string.partner_sms_validity_note), OTPManager.validityMinutes()));
        manager.clearOTP();
    }
}
