package com.grepguru.zenlock.ui;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class ScreenInsets {
    private ScreenInsets() {}

    public static void enable(ComponentActivity activity) {
        boolean light = activity.getResources().getBoolean(com.grepguru.zenlock.R.bool.light_system_bars);
        SystemBarStyle bars = light ? SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                : SystemBarStyle.dark(Color.TRANSPARENT);
        EdgeToEdge.enable(activity, bars, bars);
    }

    public static void applyToContent(ComponentActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        apply(content.getChildAt(0));
    }

    public static void apply(View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
