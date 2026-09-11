package com.grepguru.zenlock.ui.interaction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

import java.text.NumberFormat;

/** Visual-only overlay: the button retains its touch stream throughout the hold. */
final class UnlockHoldOverlay extends View {
    private final View anchor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NumberFormat numbers = NumberFormat.getIntegerInstance();
    private ViewGroup host;
    private long remaining = HoldGate.DURATION_MS;

    UnlockHoldOverlay(View anchor) {
        super(anchor.getContext());
        this.anchor = anchor;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
    }

    void show() {
        dismiss();
        View root = anchor.getRootView();
        if (!(root instanceof ViewGroup)) return;
        host = (ViewGroup) root;
        remaining = HoldGate.DURATION_MS;
        layout(0, 0, host.getWidth(), host.getHeight());
        host.getOverlay().add(this);
        invalidate();
    }

    void update(long remaining) {
        this.remaining = remaining;
        invalidate();
    }

    void dismiss() {
        if (host != null) {
            host.getOverlay().remove(this);
            host = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xF014181D);
        float dp = getResources().getDisplayMetrics().density;
        float x = getWidth() / 2f;
        float y = getHeight() / 2f - 24 * dp;
        float radius = 58 * dp;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(3 * dp);
        paint.setColor(0x33FFFFFF);
        canvas.drawCircle(x, y, radius, paint);
        paint.setColor(Color.WHITE);
        float progress = 1f - remaining / (float) HoldGate.DURATION_MS;
        canvas.drawArc(x - radius, y - radius, x + radius, y + radius,
                -90, 360 * progress, false, paint);

        // A simple clock face stays still while the outer ring fills.
        paint.setStrokeWidth(2 * dp);
        canvas.drawCircle(x, y, 20 * dp, paint);
        canvas.drawLine(x, y, x, y - 11 * dp, paint);
        canvas.drawLine(x, y, x + 9 * dp, y + 5 * dp, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(36 * getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText(numbers.format((remaining + 999) / 1000), x, y + 110 * dp, paint);
    }
}
