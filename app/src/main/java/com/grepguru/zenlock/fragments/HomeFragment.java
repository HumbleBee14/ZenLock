package com.grepguru.zenlock.fragments;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
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
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.grepguru.zenlock.LockScreenActivity;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.utils.AnalyticsManager;

public class HomeFragment extends Fragment {

    // Modern UI Elements
    private MaterialButton increaseTimeButton, decreaseTimeButton;
    private MaterialButton enableLockButton;
    private TextView selectedTimeDisplay;
    private View timeDisplayContainer;
    private Chip preset15min, preset30min, preset60min;
    
    // Hidden NumberPickers for dialog
    private NumberPicker hoursPicker, minutesPicker;
    
    private int selectedMinutes = 0; // Total minutes
    private static final int TIME_INCREMENT = 5; // 5 minutes increment
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
    private static final long ZEN_ACTIVATION_DURATION = 2000; // 2 seconds

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UI elements
        selectedTimeDisplay = view.findViewById(R.id.selectedTimeDisplay);
        timeDisplayContainer = view.findViewById(R.id.timeDisplayContainer);
        enableLockButton = view.findViewById(R.id.enableLockButton);
        increaseTimeButton = view.findViewById(R.id.increaseTimeButton);
        decreaseTimeButton = view.findViewById(R.id.decreaseTimeButton);
        
        // Preset chips
        preset15min = view.findViewById(R.id.preset15min);
        preset30min = view.findViewById(R.id.preset30min);
        preset60min = view.findViewById(R.id.preset60min);
        
        // Hidden NumberPickers
        hoursPicker = view.findViewById(R.id.hoursPicker);
        minutesPicker = view.findViewById(R.id.minutesPicker);

        // Initialize Zen Progress Overlay
        zenProgressOverlay = view.findViewById(R.id.zenProgressOverlay);
        zenCircularProgress = view.findViewById(R.id.zenCircularProgress);
        zenEmoji = view.findViewById(R.id.zenEmoji);
        zenProgressMessage = view.findViewById(R.id.zenProgressMessage);
        zenProgressSubtitle = view.findViewById(R.id.zenProgressSubtitle);

        setupNumberPickers(); // hidden but needed for dialog
        setupModernControls();
        setupZenLongPressButton();

        // Initialize analytics manager
        analyticsManager = new AnalyticsManager(requireContext());

        // Set default time
        setTimeInMinutes(10); // Default 10 minutes

        return view;
    }

    private void setupModernControls() {
        // Make time display clickable to open NumberPicker dialog
        timeDisplayContainer.setOnClickListener(v -> showTimePickerDialog());

        // +/- buttons for quick adjustments (5 min increments)
        // Single tap: +/- 5 minutes
        // Long press: Rapid auto-increment/decrement
        
        setupIncrementButton();
        setupDecrementButton();

        // Preset chips - ADD to current time instead of setting
        preset15min.setOnClickListener(v -> {
            selectedMinutes = Math.min(selectedMinutes + 15, 1440); // Add 15 minutes
            updateTimeDisplay();
        });
        
        preset30min.setOnClickListener(v -> {
            selectedMinutes = Math.min(selectedMinutes + 30, 1440); // Add 30 minutes
            updateTimeDisplay();
        });
        
        preset60min.setOnClickListener(v -> {
            selectedMinutes = Math.min(selectedMinutes + 60, 1440); // Add 1 hour
            updateTimeDisplay();
        });
    }

    private Handler autoIncrementHandler = new Handler(Looper.getMainLooper());
    private Runnable autoIncrementRunnable;
    private boolean isAutoIncrementing = false;

    private void setupIncrementButton() {
        increaseTimeButton.setOnClickListener(v -> {
            // Single tap: +5 minutes
            selectedMinutes = Math.min(selectedMinutes + TIME_INCREMENT, 1440);
            updateTimeDisplay();
        });

        increaseTimeButton.setOnLongClickListener(v -> {
            // Long press: Start auto-increment
            isAutoIncrementing = true;
            autoIncrementRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isAutoIncrementing && selectedMinutes < 1440) {
                        selectedMinutes = Math.min(selectedMinutes + TIME_INCREMENT, 1440);
                        updateTimeDisplay();
                        // Repeat every 100ms for smooth rapid increment
                        autoIncrementHandler.postDelayed(this, 100);
                    }
                }
            };
            autoIncrementHandler.post(autoIncrementRunnable);
            return true;
        });

        increaseTimeButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                // Stop auto-increment when finger is released
                isAutoIncrementing = false;
                autoIncrementHandler.removeCallbacks(autoIncrementRunnable);
            }
            return false; // Let click/long-click handlers also process
        });
    }

    private void setupDecrementButton() {
        decreaseTimeButton.setOnClickListener(v -> {
            // Single tap: -5 minutes
            selectedMinutes = Math.max(selectedMinutes - TIME_INCREMENT, 0);
            updateTimeDisplay();
        });

        decreaseTimeButton.setOnLongClickListener(v -> {
            // Long press: Start auto-decrement
            isAutoIncrementing = true;
            autoIncrementRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isAutoIncrementing && selectedMinutes > 0) {
                        selectedMinutes = Math.max(selectedMinutes - TIME_INCREMENT, 0);
                        updateTimeDisplay();
                        // Repeat every 100ms for smooth rapid decrement
                        autoIncrementHandler.postDelayed(this, 100);
                    }
                }
            };
            autoIncrementHandler.post(autoIncrementRunnable);
            return true;
        });

        decreaseTimeButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                // Stop auto-decrement when finger is released
                isAutoIncrementing = false;
                autoIncrementHandler.removeCallbacks(autoIncrementRunnable);
            }
            return false; // Let click/long-click handlers also process
        });
    }

    private void setTimeInMinutes(int minutes) {
        selectedMinutes = minutes;
        updateTimeDisplay();
        
        // Also update hidden NumberPickers for backward compatibility
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        hoursPicker.setValue(hours);
        
        // Find closest minute value in picker
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        int closestIndex = 0;
        for (int i = 0; i < minuteValues.length; i++) {
            if (minuteValues[i] == remainingMinutes) {
                closestIndex = i;
                break;
            }
        }
        minutesPicker.setValue(closestIndex);
    }

    private void updateTimeDisplay() {
        int hours = selectedMinutes / 60;
        int minutes = selectedMinutes % 60;
        
        // Format: HH:MM
        selectedTimeDisplay.setText(String.format("%02d:%02d", hours, minutes));
        
        // Update button state
        if (selectedMinutes == 0) {
            enableLockButton.setEnabled(false);
            enableLockButton.setAlpha(0.5f);
        } else {
            enableLockButton.setEnabled(true);
            enableLockButton.setAlpha(1f);
        }
    }

    private void setupNumberPickers() {
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(8);
        String[] minuteValues = {"0", "1", "5", "10", "15", "20", "30", "40", "50"};
        minutesPicker.setDisplayedValues(minuteValues);

        hoursPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateFromPickers());
        minutesPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateFromPickers());

        // Default values
        hoursPicker.setValue(0);
        minutesPicker.setValue(4); // Index 4 = 15 minutes
    }

    private void updateFromPickers() {
        int hours = hoursPicker.getValue();
        int minuteIndex = minutesPicker.getValue();
        int[] minuteValues = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        int minutes = minuteValues[minuteIndex];
        
        selectedMinutes = (hours * 60) + minutes;
        updateTimeDisplay();
    }

    private void showTimePickerDialog() {
        // Create dialog with Material theme
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.ModernAlertDialog);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        
        // Find NumberPickers in dialog
        NumberPicker dialogHoursPicker = dialogView.findViewById(R.id.dialogHoursPicker);
        NumberPicker dialogMinutesPicker = dialogView.findViewById(R.id.dialogMinutesPicker);
        
        // Setup pickers
        dialogHoursPicker.setMinValue(0);
        dialogHoursPicker.setMaxValue(23);
        dialogMinutesPicker.setMinValue(0);
        dialogMinutesPicker.setMaxValue(8);
        String[] minuteValues = {"0", "1", "5", "10", "15", "20", "30", "40", "50"};
        dialogMinutesPicker.setDisplayedValues(minuteValues);
        
        // Set current values
        int currentHours = selectedMinutes / 60;
        int currentMinutes = selectedMinutes % 60;
        dialogHoursPicker.setValue(currentHours);
        
        // Find closest minute value
        int[] minuteValuesInt = {0, 1, 5, 10, 15, 20, 30, 40, 50};
        int closestIndex = 0;
        for (int i = 0; i < minuteValuesInt.length; i++) {
            if (minuteValuesInt[i] == currentMinutes) {
                closestIndex = i;
                break;
            }
        }
        dialogMinutesPicker.setValue(closestIndex);
        
        builder.setView(dialogView)
                .setPositiveButton("Set", (dialog, which) -> {
                    int hours = dialogHoursPicker.getValue();
                    int minuteIndex = dialogMinutesPicker.getValue();
                    int minutes = minuteValuesInt[minuteIndex];
                    setTimeInMinutes((hours * 60) + minutes);
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        // Check accessibility permission first
        if (!isAccessibilityPermissionGranted()) {
            showAccessibilityDisclosureDialog();
            return;
        }

        isLongPressing = true;

        // Show overlay with smooth fade in
        zenProgressOverlay.setVisibility(View.VISIBLE);
        zenProgressOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate emoji
        animateZenEmoji();

        // Start circular progress animation
        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(ZEN_ACTIVATION_DURATION);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            zenCircularProgress.setProgress(progress);

            if (progress < 33) {
                zenProgressMessage.setText("🌱 Preparing your zen space...");
            } else {
                zenProgressMessage.setText("🧘‍♂️ Entering ZEN Mode...");
            }
        });
        progressAnimator.start();

        // Set timer to trigger activation
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

        // Delay then start lock service
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

    private void checkAndStartLockService() {
        // Check overlay permission
        if (!Settings.canDrawOverlays(getActivity())) {
            showOverlayPermissionBanner();
            return;
        }

        SharedPreferences preferences = getActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        long lockDurationMillis = selectedMinutes * 60 * 1000L;

        if (lockDurationMillis <= 0) {
            Toast.makeText(getActivity(), "Please select a valid lock time!", Toast.LENGTH_SHORT).show();
            return;
        }

        long lockStartTime = System.currentTimeMillis();
        long lockEndTime = lockStartTime + lockDurationMillis;
        long uptimeAtLock = android.os.SystemClock.elapsedRealtime();

        editor.putBoolean("isLocked", true);
        editor.putLong("lockStartTime", lockStartTime);
        editor.putLong("lockEndTime", lockEndTime);
        editor.putLong("lockTargetDuration", lockDurationMillis);
        editor.putLong("uptimeAtLock", uptimeAtLock);
        editor.putBoolean("wasDeviceRestarted", false);
        editor.apply();

        // Start analytics tracking
        analyticsManager.startSession(lockDurationMillis);

        // Start LockScreenActivity
        Intent intent = new Intent(getActivity(), LockScreenActivity.class);
        intent.putExtra("lockDuration", lockDurationMillis);
        startActivity(intent);
    }

    private void showOverlayPermissionBanner() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Permission Required")
                .setMessage("ZenLock needs 'Display over other apps' permission to show the lock screen.\\n\\nThis allows the app to block access to other apps during focus sessions.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(android.net.Uri.fromParts("package", getActivity().getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAccessibilityDisclosureDialog() {
        SharedPreferences prefs = getActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean hasShownDisclosure = prefs.getBoolean("accessibility_disclosure_shown", false);

        if (!hasShownDisclosure) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Accessibility Permission Required")
                    .setMessage("ZenLock needs Accessibility Service permission to lock your screen during focus sessions.\\n\\n" +
                            "This permission allows the app to:\\n" +
                            "• Prevent access to your device until the timer ends\\n" +
                            "• Enable complete screen lockdown functionality\\n\\n" +
                            "No personal data is collected, stored, or transmitted. All data remains on your device.\\n\\n" +
                            "You'll be taken to Android Settings to enable this permission.")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        prefs.edit().putBoolean("accessibility_disclosure_shown", true).apply();
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        Toast.makeText(getActivity(), "Please enable Accessibility for ZenLock", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Toast.makeText(getActivity(), "Accessibility permission is required for screen locking", Toast.LENGTH_LONG).show();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(getActivity(), "Please enable Accessibility for ZenLock", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onResume() {
        super.onResume();
        // Enforce lock: if locked, redirect to lock screen
        SharedPreferences preferences = requireActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);
        if (isLocked) {
            Intent lockIntent = new Intent(requireContext(), com.grepguru.zenlock.LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
            requireActivity().finish();
        }
    }
}