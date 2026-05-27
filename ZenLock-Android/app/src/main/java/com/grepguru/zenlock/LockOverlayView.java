package com.grepguru.zenlock;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class LockOverlayView extends FrameLayout {
    private final WindowManager.LayoutParams params;

    public LockOverlayView(Context context) {
        super(context);
        setBackgroundColor(Color.parseColor("#CC111827")); // Semi-transparent dark overlay
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        // Example: Add a message (customize as needed)
        TextView textView = new TextView(context);
        textView.setText("ZenLock is active\nStay focused!");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(22);
        textView.setGravity(Gravity.CENTER);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        textView.setLayoutParams(lp);
        addView(textView);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        // Use modern fullscreen flag for API 30+ or legacy flag for older versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ (Android 11+): Use modern fullscreen flag
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        } else {
            // API 29 and below: Use deprecated flag for backward compatibility
            flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return params;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Intercept all touch events to block interaction
        return true;
    }
}

