package com.grepguru.zenlock.ui;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/** One persisted appearance choice, applied before any app screen is created. */
public final class ThemePreference {
    private ThemePreference() {}
    private static final String KEY = "appearance_mode";
    public static int selectedIndex(Context context) {
        int index = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).getInt(KEY, 0);
        return index >= 0 && index <= 2 ? index : 0;
    }
    public static int nightMode(Context context) {
        switch (selectedIndex(context)) {
            case 1: return AppCompatDelegate.MODE_NIGHT_NO;
            case 2: return AppCompatDelegate.MODE_NIGHT_YES;
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
    public static void select(Context context, int index) {
        if (index < 0 || index > 2) throw new IllegalArgumentException("Unknown appearance");
        context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).edit().putInt(KEY, index).apply();
        apply(context);
    }
    public static void apply(Context context) {
        AppCompatDelegate.setDefaultNightMode(nightMode(context));
    }
}
