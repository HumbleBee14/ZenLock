package com.grepguru.zenlock.ui.interaction;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import com.grepguru.zenlock.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.NumberPicker;

/** A scrolling clock wheel whose tap opens the full clock instead of a keyboard. */
public final class ClockNumberPicker extends NumberPicker {
    private final int touchSlop;
    private final Paint selectedBandPaint = new Paint();
    private float downX, downY;
    private boolean dragged, beganIdle;
    private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

    public ClockNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        selectedBandPaint.setColorFilter(new PorterDuffColorFilter(
                context.getColor(R.color.clockSelected), PorterDuff.Mode.SRC_IN));
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setOnScrollListener((picker, state) -> scrollState = state);
    }

    @Override public void draw(Canvas canvas) {
        // Keep the native wheel's positioning, fading and scroll animation. Tint only
        // the middle row, including its EditText when Android draws that separately.
        super.draw(canvas);
        int clip = canvas.save();
        float top = getHeight() / 3f;
        float bottom = getHeight() * 2f / 3f;
        canvas.clipRect(0, top, getWidth(), bottom);
        int layer = canvas.saveLayer(0, top, getWidth(), bottom, selectedBandPaint);
        super.draw(canvas);
        canvas.restoreToCount(layer);
        canvas.restoreToCount(clip);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                dragged = false;
                beganIdle = scrollState == OnScrollListener.SCROLL_STATE_IDLE;
                break;
            case MotionEvent.ACTION_MOVE:
                dragged |= Math.abs(event.getX() - downX) > touchSlop
                        || Math.abs(event.getY() - downY) > touchSlop;
                break;
            case MotionEvent.ACTION_UP:
                if (!dragged && beganIdle
                        && event.getEventTime() - event.getDownTime() < ViewConfiguration.getLongPressTimeout()) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    performClick();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
