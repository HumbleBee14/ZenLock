package com.grepguru.zenlock.ui.timer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.grepguru.zenlock.R;

/**
 * Circular progress timer with smooth animations
 */
public class CircularTimer extends View implements TimerType {
    
    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF progressRect;
    
    private float progress = 0f;
    private String timeText = "00:00";
    private SpannableString timeSpannableText = null;
    private long totalTimeMs = 0;
    private long remainingTimeMs = 0;
    
    // Segmented circle properties
    private static final int SEGMENT_COUNT = 60; // 60 segments for smooth progress
    private float segmentAngle;
    private float segmentLength;
    private float segmentGap;
    
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
        // Background segments paint (inactive segments)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(getContext().getColor(R.color.surface));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(8f);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Progress segments paint (active segments) - WHITE
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(android.graphics.Color.WHITE);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(8f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Text paint - MUCH LARGER SIZE
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getContext().getColor(R.color.textPrimary));
        textPaint.setTextSize(88f); // Increased from 64f to 88f
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        progressRect = new RectF();
        
        // Calculate segment properties
        segmentAngle = 360f / SEGMENT_COUNT;
        segmentLength = segmentAngle * 0.7f; // 70% of segment angle for the line
        segmentGap = segmentAngle * 0.3f; // 30% gap between segments
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - 40f; // Padding from edges
        
        // Calculate how many segments should be active
        int activeSegments = (int) (progress * SEGMENT_COUNT);
        
        // Draw all segments
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float startAngle = -90f + (i * segmentAngle); // Start from top (-90 degrees)
            
            // Choose paint based on whether segment is active
            Paint segmentPaint = (i < activeSegments) ? progressPaint : backgroundPaint;
            
            // Draw the segment
            progressRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            canvas.drawArc(progressRect, startAngle, segmentLength, false, segmentPaint);
        }
        
        // Draw time text in center
        float textY = centerY + (textPaint.descent() - textPaint.ascent()) / 2f - textPaint.descent();
        if (timeSpannableText != null) {
            // Draw spannable text (for HH:MM:SS format)
            canvas.drawText(timeSpannableText, 0, timeSpannableText.length(), centerX, textY, textPaint);
        } else {
            // Draw regular text (for MM:SS format)
            canvas.drawText(timeText, centerX, textY, textPaint);
        }
    }
    
    @Override
    public View getTimerView() {
        return this;
    }
    
    @Override
    public void updateTimer(long totalTimeMs, long remainingTimeMs) {
        this.totalTimeMs = totalTimeMs;
        this.remainingTimeMs = remainingTimeMs;
        
        // Calculate progress (0.0 to 1.0)
        float newProgress = totalTimeMs > 0 ? 1.0f - (float) remainingTimeMs / totalTimeMs : 0f;
        
        // Update time text
        updateTimeText(remainingTimeMs);
        
        // Animate progress change
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        
        progressAnimator = ValueAnimator.ofFloat(progress, newProgress);
        progressAnimator.setDuration(1000); // 1 second animation
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }
    
    @Override
    public void initialize(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
        this.remainingTimeMs = totalTimeMs;
        this.progress = 0f;
        updateTimeText(remainingTimeMs);
        invalidate();
    }
    
    private void updateTimeText(long remainingTimeMs) {
        long totalMinutes = (remainingTimeMs / 1000) / 60;
        long seconds = (remainingTimeMs / 1000) % 60;
        
        if (totalMinutes >= 60) {
            // Show hours, minutes, and seconds (HH:MM:SS format with smaller seconds)
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            String timeTextStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            
            // Make seconds smaller (half size)
            timeSpannableText = new SpannableString(timeTextStr);
            timeSpannableText.setSpan(new RelativeSizeSpan(0.5f), 6, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // ":SS" part
            timeText = null; // Clear regular text
        } else {
            // Show minutes and seconds (MM:SS format - unchanged)
            timeText = String.format("%02d:%02d", totalMinutes, seconds);
            timeSpannableText = null; // Clear spannable text
        }
    }
    
    @Override
    public void cleanup() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
    }
}
