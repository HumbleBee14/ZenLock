package com.grepguru.zenlock;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.app.Dialog;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import androidx.core.app.NotificationCompat;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.zenlock.quotes.QuoteStore;
import com.grepguru.zenlock.model.*;
import com.grepguru.zenlock.ui.adapter.*;
import com.grepguru.zenlock.ui.timer.TimerType;
import com.grepguru.zenlock.ui.timer.TimerFactory;
import com.grepguru.zenlock.utils.AppUtils;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.EnhancedUnlockManager;
import com.grepguru.zenlock.utils.KeyguardUtils;
import com.grepguru.zenlock.utils.WhitelistManager;
import com.grepguru.zenlock.VibrationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockScreenActivity extends AppCompatActivity {

    private com.grepguru.zenlock.ui.interaction.UnlockHoldController unlockHold;
    private static volatile boolean isLockScreenActive = false;

    public static boolean isActive() { return isLockScreenActive; }
    private EditText pinInput;
    private SharedPreferences preferences;
    private boolean isLaunchingWhitelistedApp = false;
    private AnalyticsManager analyticsManager;
    private boolean isExpanded = false;
    private EnhancedUnlockManager unlockManager;
    private android.os.CountDownTimer countDownTimer;
    private boolean wasManuallyUnlocked = false;
    private android.os.Handler autoHideHandler;
    private Runnable autoHideRunnable;
    
    private TimerType currentTimer;
    private View timerContainer;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable quoteRotation;
    private boolean finishing = false;
    private static final long QUOTE_ROTATION_MS = 10 * 60 * 1000L;
    private static final long APPS_AUTO_COLLAPSE_MS = 6000L;
    private static final long CHROME_HIDE_MS = 5000L;
    private final Runnable chromeHide = this::hideChrome;
    
    // Persistent notification
    private static final String CHANNEL_ID = "zenlock_persistent_lock";
    private static final int NOTIFICATION_ID = 1001;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            // Dismiss keyguard if needed (API 26+)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            }
        } else if (getWindow() != null) {
            // Only use non-deprecated flag for older versions
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onCreate(savedInstanceState);
        // Prevent multiple instances
        if (isLockScreenActive) {
            Log.d("LockScreenActivity", "Lock screen already active, finishing duplicate instance");
            finish();
            return;
        }
        isLockScreenActive = true;

        // Dismiss blocker notification if it was used to launch us (MIUI fallback)
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(LockScreenService.NOTIFICATION_ID);
            nm.cancel(9999); // BLOCKER_NOTIFICATION_ID from LockScreenLauncher
        }

        preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        analyticsManager = new AnalyticsManager(this);
        unlockManager = new EnhancedUnlockManager(this);

        // Detect reboot using system uptime
        long storedUptime = preferences.getLong("uptimeAtLock", -1);
        long currentUptime = android.os.SystemClock.elapsedRealtime();

        // Check if the device restarted using uptime OR if the wasDeviceRestarted flag is set
        boolean wasRestarted = preferences.getBoolean("wasDeviceRestarted", false);
        boolean autoRestartPref = preferences.getBoolean("auto_restart", true);

        // If device was restarted (stored uptime > current uptime OR wasDeviceRestarted flag is set)
        if (storedUptime > currentUptime || wasRestarted) {
            if (!autoRestartPref) {
                // User disabled auto-restart, clear the lock
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLocked", false);
                editor.remove("lockEndTime");
                editor.putBoolean("wasDeviceRestarted", false);
                editor.apply();

                if (analyticsManager.hasActiveSession()) {
                    analyticsManager.endSession(false);
                }

                finishLockScreen();
                return;
            } else {
                // Auto-restart is enabled, clear the restart flag and continue with lock
                preferences.edit().putBoolean("wasDeviceRestarted", false).apply();
                // Continue — lock remains active and will be enforced below
            }
        }

        // Normal behaviour if the device is not restarted
        // Retrieve saved lock end time
        long lockEndTime = preferences.getLong("lockEndTime", 0);
        long currentTime = System.currentTimeMillis();

        // If no active lock or timer already expired or device restarted, exit lock screen
        if (!preferences.getBoolean("isLocked", false) || lockEndTime == 0 || currentTime >= lockEndTime) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLocked", false);
            editor.remove("lockEndTime");
            editor.apply();

            // End analytics session if active
            if (analyticsManager.hasActiveSession()) {
                analyticsManager.endSession(false); // Interrupted due to expired timer
            }

            // Return to MainActivity
            isLockScreenActive = false; // Reset flag before finishing
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishLockScreen();
            return;
        }

        // -----------------------------------------------------------
        // Setting up UI
        setContentView(R.layout.activity_lock_screen);
        View lockRoot = findViewById(R.id.lockRoot);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(lockRoot, (v, windowInsets) -> {
            androidx.core.graphics.Insets bars = windowInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        // Initializing UI Components
        pinInput = findViewById(R.id.pinInput);
        Button unlockButton = findViewById(R.id.unlockButton);
        TextView timerCountdown = findViewById(R.id.timerCountdown);
        //  TextView lockMessage = findViewById(R.id.lockscreenMessage);
        Button unlockPromptButton = findViewById(R.id.unlockPromptButton);
        ImageView unlockArrow = findViewById(R.id.unlockArrow);
        LinearLayout unlockInputsContainer = findViewById(R.id.unlockInputsContainer);
        LinearLayout expandButtonContainer = findViewById(R.id.expandButtonContainer);
        ImageView pinVisibilityToggle = findViewById(R.id.pinVisibilityToggle);
        
        // Initialize extend lock functionality
        Button extendLockButton = findViewById(R.id.extendLockButton);
        LinearLayout unlockExtendButtonContainer = findViewById(R.id.unlockExtendButtonContainer);
        
        // Start countdown timer with remaining time
        long remainingTimeMillis = lockEndTime - currentTime;
        
        // Determine target duration to preserve progress across reinstates
        long targetDuration = preferences.getLong("lockTargetDuration", 0);
        if (targetDuration <= 0) {
            long lockStartTime = preferences.getLong("lockStartTime", 0);
            if (lockStartTime > 0 && lockEndTime > lockStartTime) {
                targetDuration = lockEndTime - lockStartTime;
            } else {
                // Fallback to current remaining time (old installs)
                targetDuration = remainingTimeMillis;
            }
        }
        
        initializeTimer(targetDuration);
        bindSessionHeader();
        if (remainingTimeMillis <= 0) {
            isLockScreenActive = false; // Reset flag before finishing
            finishLockScreen();
            return;
        }

        // Single RecyclerView for all apps (default + additional)
        RecyclerView appsRecycler = findViewById(R.id.defaultAppsRecycler);
        LinearLayout noAppsContainer = findViewById(R.id.noAppsContainer);
        android.widget.ImageView expandAppsButton = findViewById(R.id.expandAppsButton);

        appsRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        appsRecycler.setNestedScrollingEnabled(false);
        appsRecycler.setItemViewCacheSize(12);

        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());

        // Separate default apps and additional apps
        Set<String> defaultApps = AppUtils.getMainDefaultApps(this);
        List<String> additionalApps = new ArrayList<>();
        
        for (String packageName : whitelistedApps) {
            if (!defaultApps.contains(packageName)) {
                additionalApps.add(packageName);
            }
        }

        // Load default apps
        List<AppModel> defaultAppModels = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (String packageName : defaultApps) {
            try {
                Drawable icon = pm.getApplicationIcon(packageName);
                String appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
                defaultAppModels.add(new AppModel(packageName, appName, true, icon));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Load additional apps
        List<AppModel> additionalAppModels = new ArrayList<>();
        for (String packageName : additionalApps) {
            try {
                Drawable icon = pm.getApplicationIcon(packageName);
                String appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
                additionalAppModels.add(new AppModel(packageName, appName, false, icon));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        java.util.Comparator<AppModel> byName = (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName());
        java.util.Collections.sort(defaultAppModels, byName);
        java.util.Collections.sort(additionalAppModels, byName);

        // Debug logging
        Log.d("LockScreen", "Default apps count: " + defaultAppModels.size());
        Log.d("LockScreen", "Additional apps count: " + additionalAppModels.size());
        Log.d("LockScreen", "Whitelisted apps: " + whitelistedApps.toString());

        // Create combined list starting with default apps only
        List<AppModel> currentAppModels = new ArrayList<>(defaultAppModels);

        // Create single adapter for the RecyclerView
        AllowedAppsAdapter appsAdapter = new AllowedAppsAdapter(this, currentAppModels);
        appsAdapter.setOnAppLaunchListener(() -> {
            isLaunchingWhitelistedApp = true;
        });

        appsRecycler.setAdapter(appsAdapter);

        // -----------------------------------------------------------
        // Setting up Apps Section

        // Always show the expand button container
        expandButtonContainer.setVisibility(View.VISIBLE);

        // Set up PIN visibility toggle
        pinVisibilityToggle.setOnClickListener(v -> {
            if (pinInput.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show PIN
                pinInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                pinVisibilityToggle.setImageResource(R.drawable.ic_eye_off);
            } else {
                // Hide PIN
                pinInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                pinVisibilityToggle.setImageResource(R.drawable.ic_eye);
            }
        });

        Runnable appsAutoCollapse = () -> {
            if (isExpanded) expandAppsButton.performClick();
        };
        appsRecycler.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                uiHandler.removeCallbacks(appsAutoCollapse);
                uiHandler.postDelayed(appsAutoCollapse, APPS_AUTO_COLLAPSE_MS);
            }
            return false;
        });
        TextView quoteView = findViewById(R.id.lockscreenMessage);
        expandAppsButton.setOnClickListener(v -> {
            uiHandler.removeCallbacks(appsAutoCollapse);
            if (!isExpanded) {
                uiHandler.postDelayed(appsAutoCollapse, APPS_AUTO_COLLAPSE_MS);
                quoteView.setVisibility(View.GONE);
                // Expand: Show all apps (default + additional)
                if (additionalAppModels.isEmpty()) {
                    // Only default apps exist, just show them
                    currentAppModels.clear();
                    currentAppModels.addAll(defaultAppModels);
                } else {
                    // Show both default and additional apps
                    currentAppModels.clear();
                    currentAppModels.addAll(defaultAppModels);
                    currentAppModels.addAll(additionalAppModels);
                }
                appsAdapter.notifyDataSetChanged();
                
                // Check if there are any apps to show
                if (currentAppModels.isEmpty()) {
                    // Show "No Apps Allowed" message
                    noAppsContainer.setVisibility(View.VISIBLE);
                    noAppsContainer.setAlpha(0f);
                    noAppsContainer.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
                    appsRecycler.setVisibility(View.GONE);
                } else {
                    // Show the RecyclerView with smooth animation
                    appsRecycler.setVisibility(View.VISIBLE);
                    appsRecycler.setAlpha(0f);
                    appsRecycler.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
                    noAppsContainer.setVisibility(View.GONE);
                }
                
            } else {
                appsRecycler.setVisibility(View.GONE);
                noAppsContainer.setVisibility(View.GONE);
                boolean quotesEnabled = preferences.getBoolean("show_quotes", true) && QuoteStore.hasQuotes(this);
                quoteView.setVisibility(quotesEnabled && !finishing ? View.VISIBLE : View.GONE);
            }

            isExpanded = !isExpanded;
            if (isExpanded) uiHandler.removeCallbacks(chromeHide); else armChromeHide();
            expandAppsButton.setContentDescription(getString(isExpanded ? R.string.hide_allowed_apps : R.string.show_allowed_apps));

            // Rotate the expand icon (0° for expanded pointing down, 180° for collapsed pointing up)
            expandAppsButton.animate()
                .rotation(isExpanded ? 0 : 180)
                .setDuration(300)
                .start();
        });


        // -----------------------------------------------------------
        // Setting up Click Listeners

        // Initially Hide PIN Input and Keep Apps Visible
        unlockInputsContainer.setVisibility(View.GONE);
        // appsSection.setVisibility(View.VISIBLE); // This line is removed

        // Set up enhanced unlock manager
        unlockManager.setOnUnlockListener(new EnhancedUnlockManager.OnUnlockListener() {
            @Override
            public void onUnlockSuccess(UnlockMethod method) {
                handleUnlockSuccess(method);
            }
            
            @Override
            public void onUnlockCancelled() {
            }
        });

        unlockHold = new com.grepguru.zenlock.ui.interaction.UnlockHoldController(unlockPromptButton, () -> {
            if (autoHideHandler != null && autoHideRunnable != null) autoHideHandler.removeCallbacks(autoHideRunnable);
        }, () -> unlockManager.showUnlockDialog(), () -> {
            if (autoHideHandler != null && autoHideRunnable != null) {
                autoHideHandler.removeCallbacks(autoHideRunnable);
                autoHideHandler.postDelayed(autoHideRunnable, 5000);
            }
        });

        // Set up unlock arrow click listener
        unlockArrow.setOnClickListener(v -> showUnlockButton());
        findViewById(R.id.mainContentContainer).setOnClickListener(v -> showChrome());
        lockRoot.setOnClickListener(v -> showChrome());
        armChromeHide();

        // Set up extend button click listener
        extendLockButton.setOnClickListener(v -> {
            showExtendDialog();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if unlock input screen is visible
                if (unlockInputsContainer.getVisibility() == View.VISIBLE) {
                    // Hide unlock inputs and show main lock screen
                    unlockInputsContainer.setVisibility(View.GONE);
                    expandButtonContainer.setVisibility(View.VISIBLE);
                    hideUnlockButton(); // Hide unlock button and show arrow

                    pinInput.setText("");
                } else {
                    // Prevent exiting the app
                    Toast.makeText(LockScreenActivity.this, "Cannot exit Focus Mode!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up motivational quotes
        setupMotivationalQuotes();

        // Start Countdown Timer
        startCountdownTimer(targetDuration, remainingTimeMillis);
        
        // Create persistent notification if enabled
        createPersistentNotificationIfEnabled();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(LockScreenService.NOTIFICATION_ID);
    }

    @Override
    protected void onPause() {
        if (unlockHold != null) unlockHold.cancel();
        super.onPause();
        if (finishing) return;

        // If we're launching a whitelisted app, don't restart the lock screen immediately
        if (isLaunchingWhitelistedApp) {
            isLaunchingWhitelistedApp = false; // Reset the flag
            Log.d("LockScreenActivity", "Whitelisted app launch detected. Not restarting on pause.");
            return;
        }

        // Check if screen is off — if so, don't restart. The screen turning off triggers
        // onPause, and restarting would call setTurnScreenOn(true) which turns the screen
        // back on, creating an infinite wake loop. AppBlockerService will catch any app
        // when the user wakes the device.
        if (!isScreenOn()) {
            Log.d("LockScreenActivity", "Screen is off. Not restarting on pause.");
            return;
        }

        // Check if system lock screen (Keyguard) is active - if so, don't restart
        if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(this, "System Keyguard is active. Not restarting on pause.")) {
            return;
        }

        // Check if AppBlockerService recently allowed a whitelisted app
        long lastWhitelistedAppTime = preferences.getLong("lastWhitelistedAppTime", 0);
        long currentTime = System.currentTimeMillis();
        if (lastWhitelistedAppTime > 0 && (currentTime - lastWhitelistedAppTime) < 5000) { // Within last 5 seconds
            Log.d("LockScreenActivity", "AppBlockerService recently allowed whitelisted app. Not restarting on pause.");
            return;
        }

        // Check if a whitelisted app is currently in the foreground
        if (isWhitelistedAppInForeground()) {
            Log.d("LockScreenActivity", "Whitelisted app is in foreground. Not restarting on pause.");
            return;
        }

        // Only restart if we're not already finishing and this is a legitimate pause
        if (!isFinishing() && !isDestroyed()) {
            // Use a longer delay to prevent rapid restarts
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    // Check screen is still on before restarting
                    if (!isScreenOn()) {
                        Log.d("LockScreenActivity", "Screen turned off. Canceling restart.");
                        return;
                    }

                    // Double-check keyguard state before restarting
                    if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(LockScreenActivity.this, "System Keyguard is active. Canceling restart.")) {
                        return;
                    }

                    // Double-check if AppBlockerService recently allowed a whitelisted app
                    long lastWhitelistedAppTime2 = preferences.getLong("lastWhitelistedAppTime", 0);
                    long currentTime2 = System.currentTimeMillis();
                    if (lastWhitelistedAppTime2 > 0 && (currentTime2 - lastWhitelistedAppTime2) < 5000) {
                        Log.d("LockScreenActivity", "AppBlockerService recently allowed whitelisted app. Canceling restart.");
                        return;
                    }

                    // Double-check if whitelisted app is still in foreground
                    if (isWhitelistedAppInForeground()) {
                        Log.d("LockScreenActivity", "Whitelisted app still in foreground. Canceling restart.");
                        return;
                    }

                    Intent intent = new Intent(this, LockScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }, 150); // Increased delay to 150ms
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // If we're launching a whitelisted app, don't restart the lock screen immediately
        if (isLaunchingWhitelistedApp) {
            Log.d("LockScreenActivity", "Whitelisted app launch detected. Not restarting on stop.");
            return;
        }

        // Don't restart if screen is off (same reason as onPause — prevents wake loop)
        if (!isScreenOn()) {
            Log.d("LockScreenActivity", "Screen is off. Not restarting on stop.");
            return;
        }

        // Check if system lock screen (Keyguard) is active - if so, don't restart
        if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(this, "System Keyguard is active. Not restarting on stop.")) {
            return;
        }

        // Check if AppBlockerService recently allowed a whitelisted app
        long lastWhitelistedAppTime = preferences.getLong("lastWhitelistedAppTime", 0);
        long currentTime = System.currentTimeMillis();
        if (lastWhitelistedAppTime > 0 && (currentTime - lastWhitelistedAppTime) < 5000) { // Within last 5 seconds
            Log.d("LockScreenActivity", "AppBlockerService recently allowed whitelisted app. Not restarting on stop.");
            return;
        }

        // Check if a whitelisted app is currently in the foreground
        if (isWhitelistedAppInForeground()) {
            Log.d("LockScreenActivity", "Whitelisted app is in foreground. Not restarting on stop.");
            return;
        }

        // Only restart if we're not already finishing
        if (!isFinishing() && !isDestroyed()) {
            // Use a longer delay to prevent rapid restarts
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    // Check screen is still on
                    if (!isScreenOn()) {
                        Log.d("LockScreenActivity", "Screen turned off. Canceling restart.");
                        return;
                    }

                    // Double-check keyguard state before restarting
                    if (KeyguardUtils.shouldReturnEarlyDueToKeyguard(LockScreenActivity.this, "System Keyguard is active. Canceling restart.")) {
                        return;
                    }

                    // Double-check if AppBlockerService recently allowed a whitelisted app
                    long lastWhitelistedAppTime2 = preferences.getLong("lastWhitelistedAppTime", 0);
                    long currentTime2 = System.currentTimeMillis();
                    if (lastWhitelistedAppTime2 > 0 && (currentTime2 - lastWhitelistedAppTime2) < 5000) {
                        Log.d("LockScreenActivity", "AppBlockerService recently allowed whitelisted app. Canceling restart.");
                        return;
                    }

                    // Double-check if whitelisted app is still in foreground
                    if (isWhitelistedAppInForeground()) {
                        Log.d("LockScreenActivity", "Whitelisted app still in foreground. Canceling restart.");
                        return;
                    }

                    Intent intent = new Intent(this, LockScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }, 100); // 0.1 second delay for onStop
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always bring lock screen to front if not already
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.moveTaskToFront(getTaskId(), 0);
        }

        // Ensure persistent notification is always visible
        createPersistentNotificationIfEnabled();

        // If lock screen lost focus, restart it instantly
        if (!isLockScreenActive) {
            Intent intent = new Intent(this, LockScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && unlockHold != null) unlockHold.cancel();

        // Remove automatic restart on focus change to prevent loops
        // The onPause/onStop methods will handle legitimate cases where user tries to leave
    }

    /**
     * Show unlock button with auto-hide after 5 seconds
     */
    private void showUnlockButton() {
        Button unlockPromptButton = findViewById(R.id.unlockPromptButton);
        ImageView unlockArrow = findViewById(R.id.unlockArrow);
        LinearLayout unlockExtendButtonContainer = findViewById(R.id.unlockExtendButtonContainer);

        if (autoHideHandler != null && autoHideRunnable != null) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
        }
        uiHandler.removeCallbacks(chromeHide);

        unlockExtendButtonContainer.setVisibility(View.VISIBLE);
        unlockExtendButtonContainer.setTranslationY(50f); // Start slightly below
        unlockExtendButtonContainer.setAlpha(0f);
        unlockExtendButtonContainer.animate()
            .translationY(0f) // Slide up to final position
            .alpha(1f) // Fade in
            .setDuration(300)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();

        // Hide arrow with fade out
        unlockArrow.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction(() -> unlockArrow.setVisibility(View.GONE))
            .start();

        // Set up auto-hide after 5 seconds
        autoHideHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoHideRunnable = () -> {
            hideUnlockButton();
        };
        autoHideHandler.postDelayed(autoHideRunnable, 5000); // 5 seconds
    }

    /**
     * Hide unlock button and show arrow
     */
    private void hideUnlockButton() {
        if (unlockHold != null) unlockHold.cancel();
        Button unlockPromptButton = findViewById(R.id.unlockPromptButton);
        ImageView unlockArrow = findViewById(R.id.unlockArrow);
        LinearLayout unlockExtendButtonContainer = findViewById(R.id.unlockExtendButtonContainer);

        // Hide unlock and extend button container with smooth slide-down animation
        unlockExtendButtonContainer.animate()
            .translationY(50f) // Slide down slightly
            .alpha(0f) // Fade out
            .setDuration(300)
            .setInterpolator(new android.view.animation.AccelerateInterpolator())
            .withEndAction(() -> {
                unlockExtendButtonContainer.setVisibility(View.GONE);
                unlockExtendButtonContainer.setTranslationY(0f); // Reset position for next time
            })
            .start();

        unlockArrow.setVisibility(View.VISIBLE);
        unlockArrow.setAlpha(0f);
        unlockArrow.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        armChromeHide();
    }

    private View[] chromeViews() {
        return new View[]{findViewById(R.id.unlockArrow), findViewById(R.id.expandButtonContainer)};
    }

    private void armChromeHide() {
        uiHandler.removeCallbacks(chromeHide);
        uiHandler.postDelayed(chromeHide, CHROME_HIDE_MS);
    }

    private void hideChrome() {
        if (finishing || isExpanded) return;
        if (findViewById(R.id.unlockExtendButtonContainer).getVisibility() == View.VISIBLE) return;
        for (View view : chromeViews()) {
            if (view == null || view.getVisibility() != View.VISIBLE) continue;
            view.animate().alpha(0f).setDuration(400)
                .withEndAction(() -> { if (view.getAlpha() == 0f) view.setVisibility(View.INVISIBLE); })
                .start();
        }
    }

    private void showChrome() {
        if (finishing) return;
        for (View view : chromeViews()) {
            if (view == null || view.getVisibility() == View.GONE) continue;
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).setDuration(250).start();
        }
        armChromeHide();
    }

    @Override
    protected void onDestroy() {
        if (unlockHold != null) unlockHold.cancel();
        super.onDestroy();
        // Always reset the flag when activity is destroyed
        isLockScreenActive = false;

        // Cancel countdown timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (autoHideHandler != null && autoHideRunnable != null) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
        }
        uiHandler.removeCallbacksAndMessages(null);

        if (currentTimer != null) {
            currentTimer.cleanup();
        }

        // Cleanup unlock manager
        if (unlockManager != null) {
            unlockManager.cleanup();
        }
    }

    /**
     * Check if the device screen is currently on.
     * Used to prevent restarting LockScreenActivity when the screen turns off,
     * which would cause an infinite wake loop due to setTurnScreenOn(true).
     */
    private boolean isScreenOn() {
        try {
            android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isInteractive();
        } catch (Exception e) {
            return true; // Assume screen is on if check fails
        }
    }

    /**
     * Check if a whitelisted app is currently in the foreground.
     * This prevents LockScreenActivity from restarting when user is using allowed apps.
     */
    private boolean isWhitelistedAppInForeground() {
        try {
            // Use ActivityManager to get running processes
            android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                java.util.List<android.app.ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
                if (runningProcesses != null) {
                    for (android.app.ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                        if (processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            String foregroundPackage = processInfo.processName;

                            // Check if this is a whitelisted app
                            if (WhitelistManager.isAppWhitelisted(LockScreenActivity.this, foregroundPackage)) {
                                Log.d("LockScreenActivity", "Whitelisted app detected in foreground: " + foregroundPackage);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("LockScreenActivity", "Error checking foreground app", e);
        }
        return false;
    }



    private void handleUnlockSuccess(UnlockMethod method) {
        // Vibrate for feedback if enabled
        VibrationUtils.vibrate(this, 50);
        Toast.makeText(this, "Unlocked via " + method.getDisplayName(), Toast.LENGTH_SHORT).show();

        // Mark as manually unlocked to prevent completion toast
        wasManuallyUnlocked = true;

        // Cancel the countdown timer to prevent it from finishing
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // End analytics session
        if (analyticsManager.hasActiveSession()) {
            analyticsManager.endSession(false); // Interrupted by manual unlock
        }

        // Reset lock state
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLocked", false); // Mark as unlocked
        editor.remove("lockEndTime");
        editor.apply();

        // Return to MainActivity
        isLockScreenActive = false; // Reset flag before finishing
        Intent intent = new Intent(LockScreenActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishLockScreen();
    }


    private void setupMotivationalQuotes() {
        TextView lockscreenMessage = findViewById(R.id.lockscreenMessage);
        String quote = preferences.getBoolean("show_quotes", true) ? QuoteStore.random(this) : null;
        if (quote == null) {
            lockscreenMessage.setVisibility(View.GONE);
            return;
        }
        lockscreenMessage.setText(quote);
        if (QuoteStore.all(this).size() < 2) return;
        quoteRotation = () -> {
            rotateQuote(lockscreenMessage);
            uiHandler.postDelayed(quoteRotation, QUOTE_ROTATION_MS);
        };
        uiHandler.postDelayed(quoteRotation, QUOTE_ROTATION_MS);
    }

    private void rotateQuote(TextView lockscreenMessage) {
        String current = lockscreenMessage.getText().toString();
        String next = QuoteStore.random(this);
        for (int i = 0; i < 5 && next != null && next.equals(current); i++) next = QuoteStore.random(this);
        if (next == null || next.equals(current)) return;
        String chosen = next;
        lockscreenMessage.animate().alpha(0f).setDuration(400).withEndAction(() -> {
            lockscreenMessage.setText(chosen);
            lockscreenMessage.animate().alpha(1f).setDuration(400).start();
        }).start();
    }

    private void bindSessionHeader() {
        TextView sessionTitle = findViewById(R.id.sessionTitle);
        String source = preferences.getString("current_session_source", "");
        String name = source.startsWith("schedule:") ? source.substring("schedule:".length()).trim() : "";
        sessionTitle.setText(name.isEmpty() ? "Focus" : name);
        updateEndsAt();
    }

    private void updateEndsAt() {
        TextView endsAtText = findViewById(R.id.endsAtText);
        long lockEndTime = preferences.getLong("lockEndTime", 0);
        if (endsAtText == null || lockEndTime <= 0) return;
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(lockEndTime));
        Calendar now = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(lockEndTime);
        boolean sameDay = now.get(Calendar.YEAR) == end.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR);
        endsAtText.setText(sameDay ? "Ends at " + time : "Ends tomorrow at " + time);
    }

    private void updateProgress(long totalTimeMs, long remainingTimeMs) {
        View fill = findViewById(R.id.timerProgressFill);
        if (fill == null || totalTimeMs <= 0) return;
        float progress = 1f - (float) remainingTimeMs / totalTimeMs;
        fill.setPivotX(0f);
        fill.setScaleX(Math.max(0f, Math.min(1f, progress)));
    }

    private static String formatDuration(long millis) {
        long minutes = Math.max(1, Math.round(millis / 60000.0));
        long hours = minutes / 60;
        minutes %= 60;
        if (hours == 0) return minutes + "m";
        if (minutes == 0) return hours + "h";
        return hours + "h " + minutes + "m";
    }

    private void showFinishMoment(long totalTimeMs) {
        finishing = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (quoteRotation != null) uiHandler.removeCallbacks(quoteRotation);
        if (autoHideHandler != null && autoHideRunnable != null) autoHideHandler.removeCallbacks(autoHideRunnable);
        if (unlockHold != null) unlockHold.cancel();
        if (currentTimer != null) currentTimer.updateTimer(totalTimeMs, 0);
        updateProgress(totalTimeMs, 0);

        TextView sessionTitle = findViewById(R.id.sessionTitle);
        TextView endsAtText = findViewById(R.id.endsAtText);
        sessionTitle.setText("Done");
        endsAtText.setText(formatDuration(totalTimeMs) + " focused");
        findViewById(R.id.mainContentContainer).setClickable(false);

        int[] fade = {R.id.lockscreenMessage, R.id.unlockArrow, R.id.unlockExtendButtonContainer,
                R.id.unlockInputsContainer, R.id.parentAppsContainer, R.id.expandButtonContainer};
        for (int id : fade) {
            View view = findViewById(id);
            if (view != null && view.getVisibility() == View.VISIBLE) view.animate().alpha(0f).setDuration(300).start();
        }
        uiHandler.postDelayed(this::finishLockScreen, 1800);
    }

    /**
     * Initialize the timer system based on user preferences
     */
    private void initializeTimer(long totalTimeMs) {
        // Get timer style from preferences
        String timerStyle = preferences.getString("timer_style", "digital");

        // Create timer instance
        currentTimer = TimerFactory.createTimer(this, timerStyle);

        // Get timer container and replace the default timer
        timerContainer = findViewById(R.id.timerContainer);
        if (timerContainer != null && timerContainer instanceof android.widget.FrameLayout) {
            android.widget.FrameLayout frameLayout = (android.widget.FrameLayout) timerContainer;
            frameLayout.removeAllViews();
            frameLayout.addView(currentTimer.getTimerView(), new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));

            // Bound the timer to available width, including narrow and split-screen windows.
            android.view.ViewGroup.LayoutParams params = frameLayout.getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            int availableWidth = getResources().getDisplayMetrics().widthPixels - (int) (48 * density);
            params.width = Math.min((int) (280 * density), availableWidth);
            boolean circular = "circular".equals(timerStyle);
            params.height = circular ? params.width : (int) (100 * density);
            frameLayout.setLayoutParams(params);

            View track = findViewById(R.id.timerProgressTrack);
            if (track != null) {
                track.getLayoutParams().width = params.width - (int) (40 * density);
                track.setVisibility(circular ? View.GONE : View.VISIBLE);
            }
        }

        currentTimer.initialize(totalTimeMs);
    }

    private void startCountdownTimer(long totalTimeMs, long remainingTimeMillis) {
        countDownTimer = new android.os.CountDownTimer(remainingTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (currentTimer != null) {
                    currentTimer.updateTimer(totalTimeMs, millisUntilFinished);
                }
                updateProgress(totalTimeMs, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                long persistedEndTime = preferences.getLong("lockEndTime", 0);
                long currentTime = System.currentTimeMillis();

                // If the session was extended elsewhere, this timer instance is stale.
                // Restart from the persisted state instead of ending the focus session early.
                if (preferences.getBoolean("isLocked", false) && persistedEndTime > currentTime) {
                    long remainingTimeMillis = persistedEndTime - currentTime;
                    long persistedTargetDuration = preferences.getLong("lockTargetDuration", 0);
                    if (persistedTargetDuration <= 0) {
                        long persistedStartTime = preferences.getLong("lockStartTime", 0);
                        if (persistedStartTime > 0 && persistedEndTime > persistedStartTime) {
                            persistedTargetDuration = persistedEndTime - persistedStartTime;
                        } else {
                            persistedTargetDuration = remainingTimeMillis;
                        }
                    }

                    updateTimerDisplay(remainingTimeMillis);
                    startCountdownTimer(persistedTargetDuration, remainingTimeMillis);
                    updatePersistentNotification();
                    return;
                }

                // Only show completion toast if not manually unlocked
                if (!wasManuallyUnlocked) {
                    // End analytics session
                    if (analyticsManager.hasActiveSession()) {
                        analyticsManager.endSession(true); // Completed successfully
                    }

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isLocked", false);
                    editor.remove("lockEndTime"); // Remove saved lock end time
                    editor.apply();

                    VibrationUtils.vibrate(LockScreenActivity.this, 500);
                    showFinishMoment(totalTimeMs);
                    return;
                }
                finishLockScreen();
            }
        };
        countDownTimer.start();
    }

    private void finishLockScreen() {
        // Call this when lock ends (unlock, timer expires, etc.)
        isLockScreenActive = false;
        
        // Remove persistent notification
        removePersistentNotification();
        
        // Clear any pre-notifications for this session
        clearPreNotificationsForCurrentSession();
        
                finish();
            }

    private void showExtendDialog() {
        Context themed = new android.view.ContextThemeWrapper(this, R.style.Theme_ZenLock);
        View dialogView = android.view.LayoutInflater.from(themed).inflate(R.layout.dialog_extend, null);
        Chip[] chips = {dialogView.findViewById(R.id.extend15Chip), dialogView.findViewById(R.id.extend30Chip),
                dialogView.findViewById(R.id.extend60Chip), dialogView.findViewById(R.id.extendCustomChip)};
        int[] presetMinutes = {15, 30, 60};
        View pickers = dialogView.findViewById(R.id.extendPickers);
        NumberPicker hoursPicker = dialogView.findViewById(R.id.hoursPicker);
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);

        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(5);
        hoursPicker.setValue(0);
        String[] minuteValues = {"0", "5", "10", "15", "20", "30", "45"};
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(minuteValues.length - 1);
        minutesPicker.setDisplayedValues(minuteValues);
        minutesPicker.setValue(3);

        int[] selected = {0};
        for (int i = 0; i < chips.length; i++) {
            int index = i;
            chips[i].setOnClickListener(v -> {
                selected[0] = index;
                for (int j = 0; j < chips.length; j++) chips[j].setChecked(j == index);
                pickers.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
            });
        }
        chips[0].setChecked(true);

        Dialog dialog = new Dialog(themed);
        dialog.setContentView(dialogView);
        com.grepguru.zenlock.ui.Popups.size(dialog);

        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.extendButton).setOnClickListener(v -> {
            long minutes = selected[0] < 3
                    ? presetMinutes[selected[0]]
                    : hoursPicker.getValue() * 60L + Integer.parseInt(minuteValues[minutesPicker.getValue()]);
            if (minutes <= 0) {
                Toast.makeText(this, "Pick a duration", Toast.LENGTH_SHORT).show();
                return;
            }
            extendLockDuration(minutes * 60 * 1000L);
            dialog.dismiss();
        });
        dialog.show();
    }

    /**
     * Extend the current lock duration
     */
    private void extendLockDuration(long extraMillis) {
        long lockEndTime = preferences.getLong("lockEndTime", 0);
        long newEndTime = lockEndTime + extraMillis;
        preferences.edit().putLong("lockEndTime", newEndTime).apply();
        
        long lockStartTime = preferences.getLong("lockStartTime", 0);
        long targetDuration = preferences.getLong("lockTargetDuration", 0) + extraMillis;
        preferences.edit().putLong("lockTargetDuration", targetDuration).apply();
        
        long currentTime = System.currentTimeMillis();
        long remainingTimeMillis = newEndTime - currentTime;

        // Restart timer with new duration
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        startCountdownTimer(targetDuration, remainingTimeMillis);
        
        updateTimerDisplay(remainingTimeMillis);
        updateEndsAt();
        updatePersistentNotification();
    }

    private void updateTimerDisplay(long remainingTimeMillis) {
        long totalTimeMs = preferences.getLong("lockTargetDuration", remainingTimeMillis);
        if (currentTimer != null) currentTimer.updateTimer(totalTimeMs, remainingTimeMillis);
        updateProgress(totalTimeMs, remainingTimeMillis);
    }

    /**
     * Create persistent notification if enabled in settings
     */
    private void createPersistentNotificationIfEnabled() {
        boolean persistentNotificationEnabled = preferences.getBoolean("persistent_notification", true);
        if (!persistentNotificationEnabled) {
            return;
        }
        
        try {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                Log.e("LockScreenActivity", "NotificationManager is null");
                return;
            }
            
            // Create notification channel for Android 8.0+
            createNotificationChannel();
            
            // Create and show notification
            Notification notification = createPersistentNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
            Log.d("LockScreenActivity", "Persistent notification created");
            
        } catch (Exception e) {
            Log.e("LockScreenActivity", "Failed to create persistent notification", e);
        }
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "ZenLock Focus Session",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Persistent notification when focus session is active");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Create persistent notification
     */
    private Notification createPersistentNotification() {
        // Create intent to open the app
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            NOTIFICATION_ID, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Get end time for display
        long lockEndTime = preferences.getLong("lockEndTime", 0);
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String endTimeText = timeFormat.format(new java.util.Date(lockEndTime));
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("ZenLock Active")
            .setContentText("Ends at " + endTimeText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .build();
    }
    
    /**
     * Update persistent notification with current end time
     */
    private void updatePersistentNotification() {
        boolean persistentNotificationEnabled = preferences.getBoolean("persistent_notification", true);
        if (!persistentNotificationEnabled || notificationManager == null) {
            return;
        }
        
        try {
            Notification notification = createPersistentNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
            Log.d("LockScreenActivity", "Persistent notification updated");
        } catch (Exception e) {
            Log.e("LockScreenActivity", "Failed to update persistent notification", e);
        }
    }
    
    /**
     * Remove persistent notification
     */
    private void removePersistentNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
            Log.d("LockScreenActivity", "Persistent notification removed");
        }
    }
    
    /**
     * Clear pre-notifications for the current session
     */
    private void clearPreNotificationsForCurrentSession() {
        try {
            // Get current session source to identify which schedule triggered this session
            String sessionSource = preferences.getString("current_session_source", "");
            if (sessionSource.startsWith("schedule:")) {
                // Extract schedule ID from session source (format: "schedule:ScheduleName")
                // For now, we'll clear all pre-notifications since we don't store schedule ID in session source
                // This is a simple approach that clears all pre-notifications when any scheduled session ends
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    // Clear all pre-notifications (they use IDs 2000+)
                    // This is safe because pre-notifications should be cleared anyway when session starts
                    for (int i = 2000; i < 3000; i++) { // Clear a reasonable range of pre-notification IDs
                        notificationManager.cancel(i);
                    }
                    Log.d("LockScreenActivity", "Cleared all pre-notifications for scheduled session");
                }
            }
        } catch (Exception e) {
            Log.e("LockScreenActivity", "Failed to clear pre-notifications", e);
        }
    }


}
