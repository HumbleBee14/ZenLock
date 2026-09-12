package com.grepguru.zenlock.ui.timer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.grepguru.zenlock.R;

public class CircularTimer extends View implements TimerType {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private float progress = 0f;
    private String mainText = "00:00";
    private String secondsText = "";
    private float strokeWidth;
    private ValueAnimator progressAnimator;

    public CircularTimer(Context context) {
        super(context);
        init();
    }

    public CircularTimer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokeWidth = dp(6);
        Typeface face = Typeface.create("sans-serif-light", Typeface.BOLD);

        trackPaint.setColor(getContext().getColor(R.color.backgroundTertiary));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint.setColor(getContext().getColor(R.color.primaryLight));
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        mainPaint.setColor(getContext().getColor(R.color.textPrimary));
        mainPaint.setTextSize(sp(56));
        mainPaint.setTypeface(face);
        mainPaint.setTextAlign(Paint.Align.CENTER);

        secondsPaint.setColor(getContext().getColor(R.color.textSecondary));
        secondsPaint.setTextSize(sp(18));
        secondsPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        secondsPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - strokeWidth;
        arcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint);
        float sweep = 360f * Math.max(0f, Math.min(1f, progress));
        if (sweep > 0f) canvas.drawArc(arcRect, -90f, sweep, false, arcPaint);

        float mainHeight = mainPaint.descent() - mainPaint.ascent();
        boolean hasSeconds = !secondsText.isEmpty();
        float secondsBlock = hasSeconds ? secondsPaint.descent() - secondsPaint.ascent() + dp(2) : 0f;
        float top = centerY - (mainHeight + secondsBlock) / 2f;
        float mainBaseline = top - mainPaint.ascent();
        canvas.drawText(mainText, centerX, mainBaseline, mainPaint);
        if (hasSeconds) {
            float secondsBaseline = mainBaseline + mainPaint.descent() + dp(2) - secondsPaint.ascent();
            canvas.drawText(secondsText, centerX, secondsBaseline, secondsPaint);
        }
    }

    @Override
    public View getTimerView() {
        return this;
    }

    @Override
    public void updateTimer(long totalTimeMs, long remainingTimeMs) {
        float target = totalTimeMs > 0 ? 1f - (float) remainingTimeMs / totalTimeMs : 0f;
        updateTimeText(remainingTimeMs);

        if (progressAnimator != null) progressAnimator.cancel();
        progressAnimator = ValueAnimator.ofFloat(progress, target);
        progressAnimator.setDuration(900);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }

    @Override
    public void initialize(long totalTimeMs) {
        progress = 0f;
        updateTimeText(totalTimeMs);
        invalidate();
    }

    private void updateTimeText(long remainingTimeMs) {
        long totalSeconds = Math.max(0, remainingTimeMs / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            mainText = String.format("%d:%02d", hours, minutes);
            secondsText = String.format("%02d", seconds);
        } else {
            mainText = String.format("%02d:%02d", minutes, seconds);
            secondsText = "";
        }
    }

    @Override
    public void cleanup() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
