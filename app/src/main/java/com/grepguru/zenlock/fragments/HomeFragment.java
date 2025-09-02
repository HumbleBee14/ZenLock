package com.grepguru.zenlock.fragments;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grepguru.zenlock.LockScreenActivity;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.utils.AnalyticsManager;

public class HomeFragment extends Fragment {

    private NumberPicker hoursPicker, minutesPicker;
    private TextView selectedTimeDisplay;
    private Button enableLockButton;
    private int selectedHours = 0, selectedMinutes = 0;
    private AnalyticsManager analyticsManager;
    
    // Zen Mode Progress Overlay Elements
    private View zenProgressOverlay;
    private ProgressBar zenCircularProgress;
    private TextView zenEmoji, zenProgressMessage, zenProgressSubtitle;
    
    // Long Press Animation Variables
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private ValueAnimator progressAnimator;
    private boolean isLongPressing = false;
    private static final long ZEN_ACTIVATION_DURATION = 3000; // 3 seconds

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        hoursPicker = view.findViewById(R.id.hoursPicker);
        minutesPicker = view.findViewById(R.id.minutesPicker);
        selectedTimeDisplay = view.findViewById(R.id.selectedTimeDisplay);
        enableLockButton = view.findViewById(R.id.enableLockButton);
        
        // Initialize Zen Progress Overlay
        zenProgressOverlay = view.findViewById(R.id.zenProgressOverlay);
        zenCircularProgress = view.findViewById(R.id.zenCircularProgress);
        zenEmoji = view.findViewById(R.id.zenEmoji);
        zenProgressMessage = view.findViewById(R.id.zenProgressMessage);
        zenProgressSubtitle = view.findViewById(R.id.zenProgressSubtitle);

        setupNumberPickers();
        setupZenLongPressButton();
        
        // Initialize analytics manager
        analyticsManager = new AnalyticsManager(requireContext());

        return view;
    }

    

    private void setupNumberPickers() {
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(8); // Updated for new values
        String[] minuteValues = {"0", "1", "5", "10", "15", "20", "30", "40", "50"}; // Added 5 & 15
        minutesPicker.setDisplayedValues(minuteValues);

        hoursPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSelectedTime());
        minutesPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSelectedTime());

        // Default values
        hoursPicker.setValue(0);
        minutesPicker.setValue(1);
        updateSelectedTime();
    }
    
    private void setupZenLongPressButton() {
        enableLockButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startZenActivation();
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelZenActivation();
                    return true;
            }
            return false;
        });
        
        // Disable normal click to prevent conflicts
        enableLockButton.setClickable(false);
    }
    
    private void startZenActivation() {
        if (isLongPressing) return;
        
        isLongPressing = true;
        
        // Show overlay with smooth fade in
        zenProgressOverlay.setVisibility(View.VISIBLE);
        zenProgressOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
                
        // Animate emoji with gentle pulse
        animateZenEmoji();
        
        // Start circular progress animation
        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(ZEN_ACTIVATION_DURATION);
        progressAnimator.setInterpolator(new LinearInterpolator()); // Linear for consistent timing
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            zenCircularProgress.setProgress(progress);
            
            // Update message: 1 second preparing, 2 seconds entering zen
            if (progress < 33) {
                zenProgressMessage.setText("🌱 Preparing your zen space...");
            } else {
                zenProgressMessage.setText("🧘‍♂️ Entering ZEN Mode...");
            }
        });
        progressAnimator.start();
        
        // Set timer to trigger zen mode activation
        longPressRunnable = () -> {
            if (isLongPressing) {
                completeZenActivation();
            }
        };
        longPressHandler.postDelayed(longPressRunnable, ZEN_ACTIVATION_DURATION);
    }
    
    private void cancelZenActivation() {
        if (!isLongPressing) return;
        
        isLongPressing = false;
        
        // Cancel timers and animations
        longPressHandler.removeCallbacks(longPressRunnable);
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        
        // Hide overlay with fade out
        zenProgressOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    zenProgressOverlay.setVisibility(View.GONE);
                    zenCircularProgress.setProgress(0);
                    zenProgressMessage.setText("Entering ZEN Mode...");
                })
                .start();
    }
    
    private void completeZenActivation() {
        isLongPressing = false;
        
        // Show completion message
        zenProgressMessage.setText("🎯 ZEN Mode Activated!");
        zenProgressSubtitle.setText("Beginning your mindful focus session");
        
        // Delay to show completion message, then start lock service
        longPressHandler.postDelayed(() -> {
            hideZenOverlay();
            checkAndStartLockService();
        }, 500);
    }
    
    private void hideZenOverlay() {
        zenProgressOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    zenProgressOverlay.setVisibility(View.GONE);
                    zenCircularProgress.setProgress(0);
                    zenProgressMessage.setText("Entering ZEN Mode...");
                    zenProgressSubtitle.setText("Hold to begin your mindful focus session");
                })
                .start();
    }
    
    private void animateZenEmoji() {
        // Gentle pulse animation for the emoji
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(zenEmoji, "scaleX", 1f, 1.1f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        ObjectAnimator pulseAnimatorY = ObjectAnimator.ofFloat(zenEmoji, "scaleY", 1f, 1.1f, 1f);
        pulseAnimatorY.setDuration(1200);
        pulseAnimatorY.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimatorY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        pulseAnimator.start();
        pulseAnimatorY.start();
    }

//    private void setupPresetButtons(View view) {
//        Button preset30min = view.findViewById(R.id.preset30min);
//        Button preset1hour = view.findViewById(R.id.preset1hour);
//        Button preset3hour = view.findViewById(R.id.preset3hour);
//
//        preset30min.setOnClickListener(v -> setPresetTime(0, 30));
//        preset1hour.setOnClickListener(v -> setPresetTime(1, 0));
//        preset3hour.setOnClickListener(v -> setPresetTime(3, 0));
//    }

    private void setPresetTime(int hours, int minutes) {
        hoursPicker.setValue(hours);
        
        // Convert minutes to picker index with new values
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        int minuteIndex = 0;
        for (int i = 0; i < minuteValues.length; i++) {
            if (minuteValues[i] == minutes) {
                minuteIndex = i;
                break;
            }
        }
        minutesPicker.setValue(minuteIndex);
        
        updateSelectedTime();
    }

    private void updateSelectedTime() {
        selectedHours = hoursPicker.getValue();
        int minuteIndex = minutesPicker.getValue();
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        selectedMinutes = minuteValues[minuteIndex];

        if (selectedHours == 0 && selectedMinutes == 0) {
            selectedTimeDisplay.setText("Set a time to start ZenLock");
            enableLockButton.setEnabled(false);
            enableLockButton.setAlpha(0.5f);
        } else {
            selectedTimeDisplay.setText("Lock Time: " + selectedHours + " hrs " + selectedMinutes + " mins");
            enableLockButton.setEnabled(true);
            enableLockButton.setAlpha(1f);
        }
    }

    private void checkAndStartLockService() {
        if (!isAccessibilityPermissionGranted()) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(getActivity(), "Please enable Accessibility for ZenLock", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        long lockDurationMillis = (selectedHours * 3600 + selectedMinutes * 60) * 1000;

        if (lockDurationMillis <= 0) {
            Toast.makeText(getActivity(), "Please select a valid lock time!", Toast.LENGTH_SHORT).show();
            return;
        }

        long lockEndTime = System.currentTimeMillis() + lockDurationMillis;
        long uptimeAtLock = android.os.SystemClock.elapsedRealtime();

        editor.putBoolean("isLocked", true);
        editor.putLong("lockEndTime", lockEndTime);
        editor.putLong("uptimeAtLock", uptimeAtLock);  // Store uptime to detect restarts (CRITICAL
        editor.putBoolean("wasDeviceRestarted", false);
        editor.apply();

        // Start analytics tracking
        analyticsManager.startSession(lockDurationMillis);

        // Start LockScreenActivity with selected duration
        Intent intent = new Intent(getActivity(), LockScreenActivity.class);
        intent.putExtra("lockDuration", lockDurationMillis);
        startActivity(intent);
    }

    private boolean isAccessibilityPermissionGranted() {
        AccessibilityManager am = (AccessibilityManager) requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (AccessibilityServiceInfo service : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (service.getId().contains(requireContext().getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

}