package com.grepguru.zenlock.ui;

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

public final class Popups {

    private Popups() {}

    public static void size(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        Context context = dialog.getContext();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int margin = Math.round(20 * metrics.density);
        int max = Math.round(400 * metrics.density);
        int width = Math.min(metrics.widthPixels - 2 * margin, max);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
