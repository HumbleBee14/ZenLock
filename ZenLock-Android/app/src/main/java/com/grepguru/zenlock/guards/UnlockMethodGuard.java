package com.grepguru.zenlock.guards;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.fragment.app.FragmentActivity;

public final class UnlockMethodGuard {

    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_RISK_ACCEPTED = "unlock_warning_accepted";
    private static final String KEY_ACKNOWLEDGED_AT = "unlock_warning_acknowledged_at";
    private static final long ACKNOWLEDGEMENT_TTL_MS = 24 * 60 * 60 * 1000L;

    private UnlockMethodGuard() {}

    public static boolean isConfigured(Context context) {
        if (PinUnlock.isEnabled(context)) return true;
        SharedPreferences prefs = prefs(context);
        return prefs.getBoolean("enable_sms_notifications", false)
            && !prefs.getString("partner_phone", "").isEmpty();
    }

    public static boolean isSatisfied(Context context) {
        if (isConfigured(context)) return true;
        SharedPreferences prefs = prefs(context);
        if (prefs.getBoolean(KEY_RISK_ACCEPTED, false)) return true;
        long acknowledgedAt = prefs.getLong(KEY_ACKNOWLEDGED_AT, 0L);
        return System.currentTimeMillis() - acknowledgedAt < ACKNOWLEDGEMENT_TTL_MS;
    }

    public static void ensure(FragmentActivity activity, String actionLabel, Runnable onProceed) {
        if (isSatisfied(activity)) {
            onProceed.run();
            return;
        }
        UnlockWarningSheet.show(activity, actionLabel, onProceed);
    }

    static void acknowledge(Context context, boolean forever) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putLong(KEY_ACKNOWLEDGED_AT, System.currentTimeMillis());
        if (forever) editor.putBoolean(KEY_RISK_ACCEPTED, true);
        editor.apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
