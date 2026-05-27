package com.grepguru.zenlock.ui.timer;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.grepguru.zenlock.R;

/**
 * Digital timer implementation - shows time as HH:MM or MM:SS
 */
public class DigitalTimer implements TimerType {
    
    private TextView timerTextView;
    private Context context;
    
    public DigitalTimer(Context context) {
        this.context = context;
    }
    
    @Override
    public View getTimerView() {
        if (timerTextView == null) {
            timerTextView = new TextView(context);
            timerTextView.setTextSize(72);
            timerTextView.setTextColor(context.getColor(R.color.textPrimary));
            timerTextView.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.BOLD));
            timerTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        return timerTextView;
    }
    
    @Override
    public void updateTimer(long totalTimeMs, long remainingTimeMs) {
        if (timerTextView == null) return;
        
        long totalMinutes = (remainingTimeMs / 1000) / 60;
        long seconds = (remainingTimeMs / 1000) % 60;
        
        if (totalMinutes >= 60) {
            // Show hours, minutes, and seconds (HH:MM ss format with smaller, regular seconds)
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            String timeText = String.format("%02d:%02d %02d", hours, minutes, seconds);
            
            // Make seconds smaller (half size) and regular font (not bold)
            SpannableString spannableString = new SpannableString(timeText);
            spannableString.setSpan(new RelativeSizeSpan(0.5f), 6, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // "ss" part
            spannableString.setSpan(new StyleSpan(Typeface.NORMAL), 6, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Make seconds regular font
            timerTextView.setText(spannableString);
        } else {
            // Show minutes and seconds (MM:SS format - unchanged)
            timerTextView.setText(String.format("%02d:%02d", totalMinutes, seconds));
        }
    }
    
    @Override
    public void initialize(long totalTimeMs) {
        // Digital timer doesn't need special initialization
    }
    
    @Override
    public void cleanup() {
        timerTextView = null;
    }
}
