package com.grepguru.zenlock.ui.timer;

import android.view.View;

/**
 * Interface for different timer display types
 * Allows easy addition of new timer styles
 */
public interface TimerType {
    
    /**
     * Get the view that displays the timer
     */
    View getTimerView();
    
    /**
     * Update the timer display with remaining time
     * @param totalTimeMs Total duration in milliseconds
     * @param remainingTimeMs Remaining time in milliseconds
     */
    void updateTimer(long totalTimeMs, long remainingTimeMs);
    
    /**
     * Initialize the timer view
     * @param totalTimeMs Total duration in milliseconds
     */
    void initialize(long totalTimeMs);
    
    /**
     * Clean up resources when timer is destroyed
     */
    void cleanup();
}
