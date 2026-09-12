package com.grepguru.zenlock.guards;

import android.content.Context;
import android.content.SharedPreferences;

public final class PinUnlock {

    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_PIN = "unlock_pin";
    private static final String KEY_ENABLED = "pin_unlock_enabled";

    private PinUnlock() {}

    public static boolean isConfigured(Context context) {
        return !prefs(context).getString(KEY_PIN, "").isEmpty();
    }

    public static boolean isEnabled(Context context) {
        return isConfigured(context) && prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static String activePin(Context context) {
        return isEnabled(context) ? prefs(context).getString(KEY_PIN, "") : "";
    }

    public static void save(Context context, String pin) {
        prefs(context).edit().putString(KEY_PIN, pin).putBoolean(KEY_ENABLED, true).apply();
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_PIN).remove(KEY_ENABLED).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
