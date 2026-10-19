package com.grepguru.zenlock.utils;

import android.content.Context;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.ui.AppLanguage;

/** Localized compact duration labels for stored analytics values. */
public final class DurationText {
    private DurationText() {}

    public static String minutes(Context context, long minutes) {
        Context localized = AppLanguage.localizedContext(context);
        if (minutes >= 60) {
            return String.format(java.util.Locale.ROOT, localized.getString(R.string.background_duration_hours_minutes), minutes / 60, minutes % 60);
        }
        return String.format(java.util.Locale.ROOT, localized.getString(R.string.background_duration_minutes), minutes);
    }

    public static String seconds(Context context, long seconds) {
        return String.format(java.util.Locale.ROOT, AppLanguage.localizedContext(context).getString(R.string.background_duration_seconds), seconds);
    }
}
