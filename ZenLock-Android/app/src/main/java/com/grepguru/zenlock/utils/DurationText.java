package com.grepguru.zenlock.utils;

import android.content.Context;
import com.grepguru.zenlock.R;

/** Compact English duration notation, independent of the UI language. */
public final class DurationText {
    private DurationText() {}

    public static String minutes(Context context, long minutes) {
        if (minutes >= 60) {
            return String.format(java.util.Locale.ROOT, context.getString(R.string.background_duration_hours_minutes), minutes / 60, minutes % 60);
        }
        return String.format(java.util.Locale.ROOT, context.getString(R.string.background_duration_minutes), minutes);
    }

    public static String seconds(Context context, long seconds) {
        return String.format(java.util.Locale.ROOT, context.getString(R.string.background_duration_seconds), seconds);
    }
}
