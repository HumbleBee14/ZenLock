package com.grepguru.zenlock.fragments;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.PopupWindow;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.grepguru.zenlock.LockScreenActivity;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.utils.AlarmPermissionManager;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.ManualStartDelayScheduler;

public class HomeFragment extends Fragment {

    private MaterialButton increaseTimeButton, decreaseTimeButton;
    private MaterialButton enableLockButton;
    private TextView selectedTimeDisplay;
    private TextView startFocusHint;
    private TextView startDelayValue;
    private View rootContentView;
    private View timeDisplayContainer;
    private View startDelayValueContainer;
    private ImageButton modeToggleButton;
    private ImageView startDelayChevron;
    private NumberPicker startDelayPicker;
    private PopupWindow startDelayPopupWindow;
    private boolean isStartDelayPickerDismissing = false;
    private int startDelayPickerInitialIndex = 0;
    private int startDelayPickerTouchStartIndex = 0;
    private float startDelayPickerTouchDownX = 0f;
    private float startDelayPickerTouchDownY = 0f;
    private boolean startDelayPickerMoved = false;
    private int startDelayPickerScrollState = NumberPicker.OnScrollListener.SCROLL_STATE_IDLE;
    private Chip preset15min, preset30min, preset60min;
    private NumberPicker hoursPicker, minutesPicker;

    // Lock Until mode
    private View lockUntilContainer;
    private View durationControlsContainer;
    private TextView lockUntilTimeDisplay;
    private boolean isLockUntilMode = false;
    private int lockUntilHour = -1;
    private int lockUntilMinute = -1;

    private int selectedMinutes = 0;
    private static final int TIME_INCREMENT = 5;
    private static final int[] START_DELAY_OPTIONS_MINUTES = {0, 1, 5, 10, 15, 20, 30, 40, 50, 60};
    private static final String[] START_DELAY_DISPLAY_VALUES = {"Now", "1m", "5m", "10m", "15m", "20m", "30m", "40m", "50m", "60m"};
    private static final String PREF_START_DELAY_SELECTION = "manual_start_delay_selection";
    private static final long START_DELAY_SCROLL_DISMISS_DELAY_MS = 260;
    private AnalyticsManager analyticsManager;
    private int selectedStartDelayMinutes = 0;

    // Zen Mode Progress Overlay Elements
    private View zenProgressOverlay;
    private ProgressBar zenCircularProgress;
    private TextView zenEmoji, zenProgressMessage, zenProgressSubtitle;

    // Long Press Animation Variables
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private ValueAnimator progressAnimator;
    private ObjectAnimator emojiPulseX, emojiPulseY;
    private boolean isLongPressing = false;
    private static final long ZEN_ACTIVATION_DURATION = 2000; // 2 seconds

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        rootContentView = view;

        SharedPreferences prefs = requireContext().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        selectedStartDelayMinutes = prefs.getInt(PREF_START_DELAY_SELECTION, 0);

        // Initialize UI elements
        selectedTimeDisplay = view.findViewById(R.id.selectedTimeDisplay);
        startFocusHint = view.findViewById(R.id.startFocusHint);
        startDelayValue = view.findViewById(R.id.startDelayValue);
        timeDisplayContainer = view.findViewById(R.id.timeDisplayContainer);
        startDelayValueContainer = view.findViewById(R.id.startDelayValueContainer);
        startDelayChevron = view.findViewById(R.id.startDelayChevron);
        enableLockButton = view.findViewById(R.id.enableLockButton);
        increaseTimeButton = view.findViewById(R.id.increaseTimeButton);
        decreaseTimeButton = view.findViewById(R.id.decreaseTimeButton);
        
        // Preset chips
        preset15min = view.findViewById(R.id.preset15min);
        preset30min = view.findViewById(R.id.preset30min);
        preset60min = view.findViewById(R.id.preset60min);
        
        hoursPicker = view.findViewById(R.id.hoursPicker);
        minutesPicker = view.findViewById(R.id.minutesPicker);

        // Lock Until mode views
        modeToggleButton = view.findViewById(R.id.modeToggleButton);
        lockUntilContainer = view.findViewById(R.id.lockUntilContainer);
        durationControlsContainer = view.findViewById(R.id.durationControlsContainer);
        lockUntilTimeDisplay = view.findViewById(R.id.lockUntilTimeDisplay);

        // Initialize Zen Progress Overlay
        zenProgressOverlay = view.findViewById(R.id.zenProgressOverlay);
        zenCircularProgress = view.findViewById(R.id.zenCircularProgress);
        zenEmoji = view.findViewById(R.id.zenEmoji);
        zenProgressMessage = view.findViewById(R.id.zenProgressMessage);
        zenProgressSubtitle = view.findViewById(R.id.zenProgressSubtitle);

        setupNumberPickers();
        setupModernControls();
        setupStartDelayControls();
        setupZenLongPressButton();
        setupModeToggle();

        analyticsManager = new AnalyticsManager(requireContext());

        setTimeInMinutes(10);

        updateStartDelayDisplay();
        if (prefs.getBoolean("lock_until_mode", false)) {
            modeToggleButton.performClick();
        }
        updateLockButtonState();

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
            tickVibrate();
            updateTimeDisplay();
        });

        preset30min.setOnClickListener(v -> {
            selectedMinutes = Math.min(selectedMinutes + 30, 1440); // Add 30 minutes
            tickVibrate();
            updateTimeDisplay();
        });

        preset60min.setOnClickListener(v -> {
            selectedMinutes = Math.min(selectedMinutes + 60, 1440); // Add 1 hour
            tickVibrate();
            updateTimeDisplay();
        });
    }

    private void setupStartDelayControls() {
        View popupContent = getLayoutInflater().inflate(R.layout.overlay_start_delay_picker, null);
        startDelayPicker = popupContent.findViewById(R.id.startDelayPicker);
        startDelayPicker.setMinValue(0);
        startDelayPicker.setMaxValue(START_DELAY_DISPLAY_VALUES.length - 1);
        startDelayPicker.setDisplayedValues(START_DELAY_DISPLAY_VALUES);
        startDelayPicker.setWrapSelectorWheel(true);
        startDelayPicker.setValue(getStartDelayIndex(selectedStartDelayMinutes));
        styleStartDelayPicker();

        startDelayPopupWindow = new PopupWindow(
                popupContent,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        startDelayPopupWindow.setOutsideTouchable(true);
        startDelayPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        startDelayPopupWindow.setOnDismissListener(
                () -> startDelayChevron.animate().rotation(0f).setDuration(160).start()
        );

        startDelayValueContainer.setOnClickListener(v -> toggleStartDelayPicker());
        startDelayPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedStartDelayMinutes = START_DELAY_OPTIONS_MINUTES[newVal];
            requireContext().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_START_DELAY_SELECTION, selectedStartDelayMinutes)
                    .apply();
            updateStartDelayDisplay();
            tickVibrate();
        });
        startDelayPicker.setOnScrollListener((picker, scrollState) -> {
            startDelayPickerScrollState = scrollState;
            if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE
                    && startDelayPopupWindow != null
                    && startDelayPopupWindow.isShowing()
                    && picker.getValue() != startDelayPickerInitialIndex) {
                picker.postDelayed(() -> {
                    if (startDelayPopupWindow != null
                            && startDelayPopupWindow.isShowing()
                            && startDelayPickerScrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE
                            && picker.getValue() != startDelayPickerInitialIndex) {
                        hideStartDelayPicker();
                    }
                }, START_DELAY_SCROLL_DISMISS_DELAY_MS);
            }
        });
        startDelayPicker.setOnTouchListener((v, event) -> {
            int touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startDelayPickerTouchDownX = event.getX();
                    startDelayPickerTouchDownY = event.getY();
                    startDelayPickerTouchStartIndex = startDelayPicker.getValue();
                    startDelayPickerMoved = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - startDelayPickerTouchDownX) > touchSlop
                            || Math.abs(event.getY() - startDelayPickerTouchDownY) > touchSlop) {
                        startDelayPickerMoved = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    startDelayPicker.postDelayed(() -> {
                        if (startDelayPopupWindow == null || !startDelayPopupWindow.isShowing()) {
                            return;
                        }
                        if (startDelayPickerScrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                            return;
                        }
                        int currentIndex = startDelayPicker.getValue();
                        if (currentIndex != startDelayPickerInitialIndex) {
                            hideStartDelayPicker();
                            return;
                        }
                        if (!startDelayPickerMoved && currentIndex == startDelayPickerTouchStartIndex) {
                            hideStartDelayPicker();
                        }
                    }, 120);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private void styleStartDelayPicker() {
        float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                15,
                requireContext().getResources().getDisplayMetrics()
        );

        for (int i = 0; i < startDelayPicker.getChildCount(); i++) {
            View child = startDelayPicker.getChildAt(i);
            if (child instanceof EditText) {
                EditText input = (EditText) child;
                input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                input.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            }
        }

        try {
            Field selectorWheelPaintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            Paint selectorWheelPaint = (Paint) selectorWheelPaintField.get(startDelayPicker);
            selectorWheelPaint.setTextSize(textSizePx);
            selectorWheelPaint.setColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            Field dividerHeightField = NumberPicker.class.getDeclaredField("mSelectionDividerHeight");
            dividerHeightField.setAccessible(true);
            dividerHeightField.set(startDelayPicker, 0);
            startDelayPicker.invalidate();
        } catch (Exception ignored) {
            // Best-effort styling for platform NumberPicker internals.
        }
    }

    private void toggleStartDelayPicker() {
        if (startDelayPopupWindow != null && startDelayPopupWindow.isShowing()) {
            hideStartDelayPicker();
        } else {
            showStartDelayPicker();
        }
    }

    private void showStartDelayPicker() {
        if (startDelayPopupWindow == null || rootContentView == null) {
            return;
        }

        startDelayPickerInitialIndex = getStartDelayIndex(selectedStartDelayMinutes);
        startDelayPickerScrollState = NumberPicker.OnScrollListener.SCROLL_STATE_IDLE;
        startDelayPicker.setValue(startDelayPickerInitialIndex);
        View popupContent = startDelayPopupWindow.getContentView();
        popupContent.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = popupContent.getMeasuredWidth();
        int popupHeight = popupContent.getMeasuredHeight();
        int gap = dpToPx(8);
        int inset = dpToPx(12);

        android.graphics.Rect visibleFrame = new android.graphics.Rect();
        rootContentView.getWindowVisibleDisplayFrame(visibleFrame);

        int[] anchorLocation = new int[2];
        startDelayValueContainer.getLocationOnScreen(anchorLocation);

        int anchorLeft = anchorLocation[0];
        int anchorTop = anchorLocation[1];
        int anchorWidth = startDelayValueContainer.getWidth();
        int anchorHeight = startDelayValueContainer.getHeight();
        int anchorCenterX = anchorLeft + (anchorWidth / 2);
        int anchorCenterY = anchorTop + (anchorHeight / 2);

        int x = anchorCenterX - (popupWidth / 2);
        int y = anchorCenterY - (popupHeight / 2);

        x = Math.max(visibleFrame.left + inset, Math.min(x, visibleFrame.right - popupWidth - inset));
        y = Math.max(visibleFrame.top + inset, Math.min(y, visibleFrame.bottom - popupHeight - inset));

        startDelayChevron.animate().rotation(180f).setDuration(160).start();

        isStartDelayPickerDismissing = false;
        startDelayPopupWindow.showAtLocation(rootContentView, Gravity.NO_GRAVITY, x, y);
        popupContent.setAlpha(0f);
        popupContent.setScaleX(0.96f);
        popupContent.setScaleY(0.96f);
        popupContent.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void hideStartDelayPicker() {
        PopupWindow popupWindow = startDelayPopupWindow;
        if (popupWindow != null && popupWindow.isShowing()) {
            if (isStartDelayPickerDismissing) {
                return;
            }
            isStartDelayPickerDismissing = true;
            View popupContent = popupWindow.getContentView();
            popupContent.animate()
                    .alpha(0f)
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(140)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        if (popupWindow.isShowing()) {
                            popupWindow.dismiss();
                        }
                        if (popupContent != null) {
                            popupContent.setAlpha(1f);
                            popupContent.setScaleX(1f);
                            popupContent.setScaleY(1f);
                        }
                        isStartDelayPickerDismissing = false;
                    })
                    .start();
        } else {
            startDelayChevron.animate().rotation(0f).setDuration(160).start();
        }
    }

    private void dismissStartDelayPickerImmediately() {
        PopupWindow popupWindow = startDelayPopupWindow;
        if (popupWindow != null) {
            View popupContent = popupWindow.getContentView();
            if (popupContent != null) {
                popupContent.animate().cancel();
                popupContent.setAlpha(1f);
                popupContent.setScaleX(1f);
                popupContent.setScaleY(1f);
            }
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        }
        isStartDelayPickerDismissing = false;
        if (startDelayChevron != null) {
            startDelayChevron.animate().cancel();
            startDelayChevron.setRotation(0f);
        }
    }

    private void setupModeToggle() {
        modeToggleButton.setOnClickListener(v -> {
            isLockUntilMode = !isLockUntilMode;
            requireContext().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("lock_until_mode", isLockUntilMode).apply();
            tickVibrate();

            if (isLockUntilMode) {
                modeToggleButton.setImageResource(R.drawable.ic_timer);
                modeToggleButton.setContentDescription("Switch to duration mode");
                timeDisplayContainer.setVisibility(View.GONE);
                durationControlsContainer.setVisibility(View.GONE);
                lockUntilContainer.setVisibility(View.VISIBLE);

                if (lockUntilHour == -1) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.HOUR_OF_DAY, 1);
                    lockUntilHour = cal.get(Calendar.HOUR_OF_DAY);
                    lockUntilMinute = 0;
                }
                updateLockUntilDisplay();
                updateLockButtonState();
            } else {
                modeToggleButton.setImageResource(R.drawable.ic_schedule);
                modeToggleButton.setContentDescription("Switch to lock until time");
                timeDisplayContainer.setVisibility(View.VISIBLE);
                durationControlsContainer.setVisibility(View.VISIBLE);
                lockUntilContainer.setVisibility(View.GONE);
                updateTimeDisplay();
            }
        });

        lockUntilContainer.setOnClickListener(v -> showLockUntilTimePicker());
    }

    private void showLockUntilTimePicker() {
        int hour = lockUntilHour != -1 ? lockUntilHour : Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1;
        int minute = lockUntilMinute != -1 ? lockUntilMinute : 0;

        android.app.TimePickerDialog picker = new android.app.TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    lockUntilHour = selectedHour;
                    lockUntilMinute = selectedMinute;
                    updateLockUntilDisplay();
                    updateLockButtonState();
                },
                hour, minute, false
        );
        picker.setTitle("Lock until");
        picker.show();
    }

    private void updateLockUntilDisplay() {
        int displayHour = lockUntilHour % 12;
        if (displayHour == 0) displayHour = 12;
        String amPm = lockUntilHour < 12 ? "AM" : "PM";
        lockUntilTimeDisplay.setText(String.format("%d:%02d %s", displayHour, lockUntilMinute, amPm));
    }

    private long getLockUntilDurationMs() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, lockUntilHour);
        target.set(Calendar.MINUTE, lockUntilMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    private long getNextLockUntilEndTimeMillis() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, lockUntilHour);
        target.set(Calendar.MINUTE, lockUntilMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        return target.getTimeInMillis();
    }

    private Handler autoIncrementHandler = new Handler(Looper.getMainLooper());
    private Runnable autoIncrementRunnable;
    private boolean isAutoIncrementing = false;

    @SuppressWarnings("deprecation")
    private void tickVibrate() {
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.os.VibratorManager vm = (android.os.VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void setupIncrementButton() {
        increaseTimeButton.setOnClickListener(v -> {
            // Single tap: +5 minutes
            selectedMinutes = Math.min(selectedMinutes + TIME_INCREMENT, 1440);
            tickVibrate();
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
            tickVibrate();
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
        
        updateLockButtonState();
    }

    private void updateStartDelayDisplay() {
        if (startDelayPicker.getValue() != getStartDelayIndex(selectedStartDelayMinutes)) {
            startDelayPicker.setValue(getStartDelayIndex(selectedStartDelayMinutes));
        }
        startDelayValue.setText(formatStartDelayLabel(selectedStartDelayMinutes));
    }

    private String formatStartDelayLabel(int minutes) {
        if (minutes <= 0) {
            return "Now";
        }
        if (minutes == 1) {
            return "In 1 min";
        }
        return "In " + minutes + " min";
    }

    private int getStartDelayIndex(int minutes) {
        for (int i = 0; i < START_DELAY_OPTIONS_MINUTES.length; i++) {
            if (START_DELAY_OPTIONS_MINUTES[i] == minutes) {
                return i;
            }
        }
        return 0;
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
            if (ManualStartDelayScheduler.hasPendingSession(requireContext())) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    updateLockButtonState();
                    Toast.makeText(requireContext(), getPendingStartBlockedMessage(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }

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
        hideStartDelayPicker();
        if (ManualStartDelayScheduler.hasPendingSession(requireContext())) {
            updateLockButtonState();
            Toast.makeText(requireContext(), getPendingStartBlockedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

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

        if (selectedStartDelayMinutes > 0) {
            zenProgressMessage.setText("⏳ Focus session scheduled");
            zenProgressSubtitle.setText(formatStartDelayLabel(selectedStartDelayMinutes));
        } else {
            zenProgressMessage.setText("🎯 ZEN Mode Activated!");
            zenProgressSubtitle.setText("Beginning your mindful focus session");
        }

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
        emojiPulseX = ObjectAnimator.ofFloat(zenEmoji, "scaleX", 1f, 1.1f, 1f);
        emojiPulseX.setDuration(1200);
        emojiPulseX.setRepeatCount(ObjectAnimator.INFINITE);
        emojiPulseX.setInterpolator(new AccelerateDecelerateInterpolator());

        emojiPulseY = ObjectAnimator.ofFloat(zenEmoji, "scaleY", 1f, 1.1f, 1f);
        emojiPulseY.setDuration(1200);
        emojiPulseY.setRepeatCount(ObjectAnimator.INFINITE);
        emojiPulseY.setInterpolator(new AccelerateDecelerateInterpolator());

        emojiPulseX.start();
        emojiPulseY.start();
    }

    @Override
    public void onDestroyView() {
        dismissStartDelayPickerImmediately();
        super.onDestroyView();
        startDelayPopupWindow = null;
        rootContentView = null;
        if (emojiPulseX != null) { emojiPulseX.cancel(); emojiPulseX = null; }
        if (emojiPulseY != null) { emojiPulseY.cancel(); emojiPulseY = null; }
        if (progressAnimator != null) { progressAnimator.cancel(); progressAnimator = null; }
        isAutoIncrementing = false;
        autoIncrementHandler.removeCallbacksAndMessages(null);
        longPressHandler.removeCallbacksAndMessages(null);
    }

    private void checkAndStartLockService() {
        // Check overlay permission
        if (!Settings.canDrawOverlays(getActivity())) {
            showOverlayPermissionBanner();
            return;
        }

        // Check notification blocking permission (non-blocking, just prompts)
        SharedPreferences preferences = getActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        if (preferences.getBoolean("block_notifications", true) && !isNotificationListenerEnabled()) {
            new android.app.AlertDialog.Builder(getActivity())
                    .setTitle("Notification Access")
                    .setMessage("Block Notifications is enabled but ZenLock doesn't have Notification Access yet.\n\nWithout this, app notifications can still appear and be used to bypass the lock.\n\nGrant access now?")
                    .setPositiveButton("Grant", (dialog, which) -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton("Skip", (dialog, which) -> {
                        // Continue with session start without notification blocking
                        proceedWithLockSession(preferences);
                    })
                    .setCancelable(false)
                    .show();
            return;
        }
        proceedWithLockSession(preferences);
    }

    private void proceedWithLockSession(SharedPreferences preferences) {
        long lockDurationMillis;
        Long absoluteLockEndTimeMillis = null;

        if (isLockUntilMode) {
            if (lockUntilHour == -1) {
                Toast.makeText(getActivity(), "Please select a lock until time!", Toast.LENGTH_SHORT).show();
                return;
            }
            absoluteLockEndTimeMillis = getNextLockUntilEndTimeMillis();
            lockDurationMillis = absoluteLockEndTimeMillis - System.currentTimeMillis();
        } else {
            lockDurationMillis = selectedMinutes * 60 * 1000L;
            if (lockDurationMillis <= 0) {
                Toast.makeText(getActivity(), "Please select a valid lock time!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (lockDurationMillis > 8 * 60 * 60 * 1000L) {
            long hours = lockDurationMillis / (60 * 60 * 1000L);
            long mins = (lockDurationMillis % (60 * 60 * 1000L)) / (60 * 1000L);
            String durationText = hours + "h " + mins + "m";
            Long finalAbsoluteLockEndTimeMillis = absoluteLockEndTimeMillis;
            new AlertDialog.Builder(getActivity())
                    .setTitle("Long Session")
                    .setMessage("You're about to lock for " + durationText + ". Are you sure?")
                    .setPositiveButton("Lock", (dialog, which) -> startOrScheduleLockSession(
                            preferences, lockDurationMillis, finalAbsoluteLockEndTimeMillis
                    ))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            startOrScheduleLockSession(preferences, lockDurationMillis, absoluteLockEndTimeMillis);
        }
    }

    private void startOrScheduleLockSession(SharedPreferences preferences, long lockDurationMillis,
                                            @Nullable Long absoluteLockEndTimeMillis) {
        if (selectedStartDelayMinutes > 0) {
            scheduleManualStart(preferences, lockDurationMillis, absoluteLockEndTimeMillis);
            return;
        }
        startLockSession(preferences, lockDurationMillis);
    }

    private void scheduleManualStart(SharedPreferences preferences, long lockDurationMillis,
                                     @Nullable Long absoluteLockEndTimeMillis) {
        if (ManualStartDelayScheduler.hasPendingSession(requireContext())) {
            updateLockButtonState();
            Toast.makeText(requireContext(), getPendingStartBlockedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!AlarmPermissionManager.canScheduleExactAlarms(requireContext())) {
            if (requireActivity() instanceof AppCompatActivity) {
                AlarmPermissionManager.requestExactAlarmPermission((AppCompatActivity) requireActivity());
            }
            return;
        }

        long scheduledStartTimeMillis = System.currentTimeMillis() + (selectedStartDelayMinutes * 60_000L);
        if (absoluteLockEndTimeMillis != null && absoluteLockEndTimeMillis <= scheduledStartTimeMillis) {
            Toast.makeText(
                    requireContext(),
                    "Pick a later lock-until time or a shorter start delay.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        boolean scheduled = ManualStartDelayScheduler.scheduleSession(
                requireContext(),
                selectedStartDelayMinutes,
                lockDurationMillis,
                absoluteLockEndTimeMillis
        );

        if (!scheduled) {
            Toast.makeText(requireContext(), "Couldn't schedule your focus session yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        preferences.edit().remove("current_session_source").apply();
        updateLockButtonState();
        Toast.makeText(
                requireContext(),
                "Focus session starts " + formatStartDelayLabel(selectedStartDelayMinutes).toLowerCase(java.util.Locale.ROOT),
                Toast.LENGTH_LONG
        ).show();
    }

    private void startLockSession(SharedPreferences preferences, long lockDurationMillis) {
        ManualStartDelayScheduler.cancelPendingSession(requireContext());

        SharedPreferences.Editor editor = preferences.edit();

        long lockStartTime = System.currentTimeMillis();
        long lockEndTime = lockStartTime + lockDurationMillis;
        long uptimeAtLock = android.os.SystemClock.elapsedRealtime();

        editor.putBoolean("isLocked", true);
        editor.putLong("lockStartTime", lockStartTime);
        editor.putLong("lockEndTime", lockEndTime);
        editor.putLong("lockTargetDuration", lockDurationMillis);
        editor.putLong("uptimeAtLock", uptimeAtLock);
        editor.putBoolean("wasDeviceRestarted", false);
        editor.putString("current_session_source", "manual");
        editor.apply();

        analyticsManager.startSession(lockDurationMillis, "manual");

        Intent intent = new Intent(getActivity(), LockScreenActivity.class);
        intent.putExtra("lockDuration", lockDurationMillis);
        startActivity(intent);
    }

    private void showOverlayPermissionBanner() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Permission Required")
                .setMessage("ZenLock needs 'Display over other apps' permission to show the lock screen.\n\nThis allows the app to block access to other apps during focus sessions.")
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
                    .setMessage("ZenLock needs Accessibility Service permission to lock your screen during focus sessions.\n\n" +
                            "This permission allows the app to:\n" +
                            "• Prevent access to your device until the timer ends\n" +
                            "• Enable complete screen lockdown functionality\n\n" +
                            "No personal data is collected, stored, or transmitted. All data remains on your device.\n\n" +
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

    private boolean isNotificationListenerEnabled() {
        String enabledListeners = android.provider.Settings.Secure.getString(
                requireContext().getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(requireContext().getPackageName());
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
            return;
        }
        updateLockButtonState();
    }

    private void updateLockButtonState() {
        if (enableLockButton == null) {
            return;
        }

        if (ManualStartDelayScheduler.hasPendingSession(requireContext())) {
            enableLockButton.setEnabled(true);
            enableLockButton.setAlpha(0.55f);
            enableLockButton.setText("Focus Scheduled");
            if (startFocusHint != null) {
                startFocusHint.setText(getPendingStartHint());
            }
            return;
        }

        enableLockButton.setText("Start Focus");
        if (startFocusHint != null) {
            startFocusHint.setText("Long press to begin");
        }

        boolean canStart = isLockUntilMode ? lockUntilHour != -1 : selectedMinutes > 0;
        enableLockButton.setEnabled(canStart);
        enableLockButton.setAlpha(canStart ? 1f : 0.5f);
    }

    private String getPendingStartHint() {
        long pendingStartAtMillis = ManualStartDelayScheduler.getPendingStartAtMillis(requireContext());
        if (pendingStartAtMillis <= 0L) {
            return "A focus session is already scheduled.";
        }
        return "Already scheduled for " + formatClockTime(pendingStartAtMillis);
    }

    private String getPendingStartBlockedMessage() {
        long pendingStartAtMillis = ManualStartDelayScheduler.getPendingStartAtMillis(requireContext());
        if (pendingStartAtMillis <= 0L) {
            return "A focus session is already scheduled.";
        }
        return "A focus session is already scheduled for " + formatClockTime(pendingStartAtMillis);
    }

    private String formatClockTime(long timeInMillis) {
        return android.text.format.DateFormat.getTimeFormat(requireContext()).format(new Date(timeInMillis));
    }
}
