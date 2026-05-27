package com.grepguru.zenlock.ui.timer;

import android.content.Context;

/**
 * Factory class for creating different timer types
 */
public class TimerFactory {
    
    public enum TimerStyle {
        DIGITAL("digital"),
        CIRCULAR("circular");
        
        private final String value;
        
        TimerStyle(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static TimerStyle fromString(String value) {
            for (TimerStyle style : TimerStyle.values()) {
                if (style.value.equals(value)) {
                    return style;
                }
            }
            return DIGITAL; // Default fallback
        }
    }
    
    /**
     * Create a timer instance based on the specified style
     */
    public static TimerType createTimer(Context context, TimerStyle style) {
        switch (style) {
            case CIRCULAR:
                return new CircularTimer(context);
            case DIGITAL:
            default:
                return new DigitalTimer(context);
        }
    }
    
    /**
     * Create a timer instance based on string value
     */
    public static TimerType createTimer(Context context, String styleValue) {
        return createTimer(context, TimerStyle.fromString(styleValue));
    }
}
