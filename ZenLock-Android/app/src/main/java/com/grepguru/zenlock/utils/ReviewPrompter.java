package com.grepguru.zenlock.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.ui.Popups;

public final class ReviewPrompter {

    private static final String PREFS = "FocusLockPrefs";
    private static final String FIRST_LAUNCH_KEY = "review_first_launch";
    private static final String ASK_COUNT_KEY = "review_ask_count";
    private static final String ACCEPTED_KEY = "review_accepted";
    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final long[] ASK_AFTER_MS = {7 * DAY_MS, 28 * DAY_MS, 56 * DAY_MS};

    private ReviewPrompter() {}

    public static boolean consumeAskIfDue(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!prefs.contains(FIRST_LAUNCH_KEY)) {
            prefs.edit().putLong(FIRST_LAUNCH_KEY, now).apply();
            return false;
        }
        if (prefs.getBoolean(ACCEPTED_KEY, false)) return false;
        int askCount = prefs.getInt(ASK_COUNT_KEY, 0);
        if (askCount >= ASK_AFTER_MS.length) return false;
        long elapsed = now - prefs.getLong(FIRST_LAUNCH_KEY, now);
        if (elapsed < ASK_AFTER_MS[askCount]) return false;
        prefs.edit().putInt(ASK_COUNT_KEY, askCount + 1).apply();
        return true;
    }

    public static void showIfDue(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (!consumeAskIfDue(activity)) return;
        Dialog dialog = new Dialog(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_review, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        view.findViewById(R.id.reviewButton).setOnClickListener(v -> {
            markAccepted(activity);
            dialog.dismiss();
            openStorePage(activity);
        });
        view.findViewById(R.id.laterButton).setOnClickListener(v -> dialog.dismiss());
        Popups.size(dialog);
        dialog.show();
    }

    private static void markAccepted(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ACCEPTED_KEY, true).apply();
    }

    private static void openStorePage(Context context) {
        String pkg = "com.grepguru.zenlock";
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }
}
