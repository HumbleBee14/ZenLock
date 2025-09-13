package com.grepguru.zenlock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.zenlock.model.*;
import com.grepguru.zenlock.ui.adapter.*;
import com.grepguru.zenlock.ui.timer.TimerType;
import com.grepguru.zenlock.ui.timer.TimerFactory;
import com.grepguru.zenlock.utils.AppUtils;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.EnhancedUnlockManager;
import com.grepguru.zenlock.utils.KeyguardUtils;
import com.grepguru.zenlock.utils.WhitelistManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockScreenActivity extends AppCompatActivity {

    private static boolean isLockScreenActive = false;
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
    
    // Timer system
    private TimerType currentTimer;
    private View timerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prevent multiple instances
        if (isLockScreenActive) {
            Log.d("LockScreenActivity", "Lock screen already active, finishing duplicate instance");
            finish();
            return;
        }
        isLockScreenActive = true;
        
        preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        analyticsManager = new AnalyticsManager(this);
        unlockManager = new EnhancedUnlockManager(this);

        // Detect reboot using system uptime
        long storedUptime = preferences.getLong("uptimeAtLock", -1);
        long currentUptime = android.os.SystemClock.elapsedRealtime();

        // Check if the device restarted using uptime OR if the wasDeviceRestarted flag is set
        boolean wasRestarted = preferences.getBoolean("wasDeviceRestarted", false);

        if (storedUptime > currentUptime || wasRestarted) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLocked", false);
            editor.remove("lockEndTime");
            editor.putBoolean("wasDeviceRestarted", false);
            editor.apply();

            // End analytics session if active
            if (analyticsManager.hasActiveSession()) {
                analyticsManager.endSession(false); // Interrupted due to restart
            }

            isLockScreenActive = false; // Reset flag before finishing
            finish();
            return;
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

            isLockScreenActive = false; // Reset flag before finishing
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // -----------------------------------------------------------
        // Setting up UI
        setContentView(R.layout.activity_lock_screen);

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
        
        // Initialize timer system with total target duration
        initializeTimer(targetDuration);
        if (remainingTimeMillis <= 0) {
            isLockScreenActive = false; // Reset flag before finishing
            finish();
            return;
        }

        // Single RecyclerView for all apps (default + additional)
        RecyclerView appsRecycler = findViewById(R.id.defaultAppsRecycler);
        LinearLayout noAppsContainer = findViewById(R.id.noAppsContainer);
        android.widget.ImageView expandAppsButton = findViewById(R.id.expandAppsButton);

        // Use GridLayoutManager for better organization - 3 apps per row
        androidx.recyclerview.widget.GridLayoutManager layoutManager = new androidx.recyclerview.widget.GridLayoutManager(this, 3);
        appsRecycler.setLayoutManager(layoutManager);

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

        // Set up expand button click listener
        expandAppsButton.setOnClickListener(v -> {
            if (!isExpanded) {
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
                
                // Animate arrow to bottom of main content container (above the apps)
                // Get the parent apps container and calculate the exact position
                View parentAppsContainer = findViewById(R.id.parentAppsContainer);
                parentAppsContainer.post(() -> {
                    int[] parentContainerLocation = new int[2];
                    parentAppsContainer.getLocationInWindow(parentContainerLocation);
                    int parentContainerTop = parentContainerLocation[1];
                    
                    int[] arrowLocation = new int[2];
                    expandButtonContainer.getLocationInWindow(arrowLocation);
                    int arrowCurrentY = arrowLocation[1];
                    
                    // Move arrow to just above the parent container (with 20dp buffer)
                    int bufferPixels = (int) (20 * getResources().getDisplayMetrics().density);
                    int targetY = parentContainerTop - bufferPixels;
                    int distanceToMove = arrowCurrentY - targetY;
                    
                    expandButtonContainer.animate()
                        .translationY(-distanceToMove)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
                });
                
                // Scroll to show all apps from the beginning after a short delay
                appsRecycler.postDelayed(() -> {
                    layoutManager.scrollToPosition(0);
                }, 100);
            } else {
                // Collapse: Hide all apps
                currentAppModels.clear();
                appsAdapter.notifyDataSetChanged();
                
                // Hide both the RecyclerView and noAppsContainer with smooth animation
                appsRecycler.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> appsRecycler.setVisibility(View.GONE))
                    .start();
                
                noAppsContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> noAppsContainer.setVisibility(View.GONE))
                    .start();
                
                // Animate arrow back to bottom position
                expandButtonContainer.animate()
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                    .start();
            }

            isExpanded = !isExpanded;

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
                // User cancelled unlock, stay in lock screen
                Toast.makeText(LockScreenActivity.this, "Unlock cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        unlockPromptButton.setOnClickListener(v -> {
            // Show enhanced unlock dialog
            unlockManager.showUnlockDialog();
        });

        // Set up unlock arrow click listener
        unlockArrow.setOnClickListener(v -> {
            showUnlockButton();
        });

        // Set up tap-to-reveal functionality on main content
        View mainContentContainer = findViewById(R.id.mainContentContainer);
        mainContentContainer.setOnClickListener(v -> {
            showUnlockButton();
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
    }

    /*
    @Override
    public void onBackPressed() {
        // Disable back button
        Toast.makeText(this, "Cannot exit Focus Mode!", Toast.LENGTH_SHORT).show();
    }
    */

    @Override
    protected void onPause() {
        super.onPause();
        
        // If we're launching a whitelisted app, don't restart the lock screen immediately
        if (isLaunchingWhitelistedApp) {
            isLaunchingWhitelistedApp = false; // Reset the flag
            Log.d("LockScreenActivity", "Whitelisted app launch detected. Not restarting on pause.");
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
            }, 500); // Increased delay to 500ms
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
            }, 1000); // 1 second delay for onStop
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Remove the automatic restart on resume to prevent loops
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        
        // Remove automatic restart on focus change to prevent loops
        // The onPause/onStop methods will handle legitimate cases where user tries to leave
    }

    /**
     * Show unlock button with auto-hide after 5 seconds
     */
    private void showUnlockButton() {
        Button unlockPromptButton = findViewById(R.id.unlockPromptButton);
        ImageView unlockArrow = findViewById(R.id.unlockArrow);
        
        // Cancel any existing auto-hide timer
        if (autoHideHandler != null && autoHideRunnable != null) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
        }
        
        // Show unlock button with animation
        unlockPromptButton.setVisibility(View.VISIBLE);
        unlockPromptButton.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Hide arrow
        unlockArrow.animate()
            .alpha(0f)
            .setDuration(200)
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
        Button unlockPromptButton = findViewById(R.id.unlockPromptButton);
        ImageView unlockArrow = findViewById(R.id.unlockArrow);
        
        // Hide unlock button with animation
        unlockPromptButton.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction(() -> unlockPromptButton.setVisibility(View.GONE))
            .start();
        
        // Show arrow
        unlockArrow.setVisibility(View.VISIBLE);
        unlockArrow.animate()
            .alpha(0.6f)
            .setDuration(300)
            .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always reset the flag when activity is destroyed
        isLockScreenActive = false;
        
        // Cancel countdown timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        // Cancel auto-hide timer
        if (autoHideHandler != null && autoHideRunnable != null) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
        }
        
        // Cleanup timer
        if (currentTimer != null) {
            currentTimer.cleanup();
        }
        
        // Cleanup unlock manager
        if (unlockManager != null) {
            unlockManager.cleanup();
        }
    }

    /**
     * Check if a whitelisted app is currently in the foreground
     * This prevents LockScreenActivity from restarting when user is using allowed apps
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
        finish();
    }


    private void setupMotivationalQuotes() {
        TextView lockscreenMessage = findViewById(R.id.lockscreenMessage);
        
        // Check if quotes are enabled
        if (!preferences.getBoolean("show_quotes", true)) {
            lockscreenMessage.setText("Stay focused, stay productive!");
            return;
        }

        // Motivational quotes array
        String[] quotes = {
            "The only way to do great work is to love what you do.",
            "Focus on being productive instead of busy.",
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "The future depends on what you do today.",
            "Don't watch the clock, do what it does. Keep going.",
            "The only limit to our realization of tomorrow is our doubts of today.",
            "It always seems impossible until it's done.",
            "The way to get started is to quit talking and begin doing.",
            "Your time is limited, don't waste it living someone else's life.",
            "The only person you are destined to become is the person you decide to be.",
            "Stay focused, stay productive!",
            "Every moment is a fresh beginning.",
            "Make today amazing!",
            "You are capable of amazing things.",
            "Focus on progress, not perfection."
        };

        // Select a random quote
        int randomIndex = (int) (Math.random() * quotes.length);
        lockscreenMessage.setText(quotes[randomIndex]);
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
            frameLayout.addView(currentTimer.getTimerView());
            
            // Adjust container size based on timer type
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) frameLayout.getLayoutParams();
            if ("circular".equals(timerStyle)) {
                // Circular timer needs more space
                params.width = (int) (350 * getResources().getDisplayMetrics().density); // Convert dp to px
                params.height = (int) (350 * getResources().getDisplayMetrics().density); // Convert dp to px
            } else {
                // Digital timer needs even less space to reduce gap with quotes
                params.width = (int) (300 * getResources().getDisplayMetrics().density); // Convert dp to px
                params.height = (int) (100 * getResources().getDisplayMetrics().density); // Reduced from 120dp to 100dp
            }
            frameLayout.setLayoutParams(params);
        }
        
        // Initialize the timer using total duration so progress reflects overall session
        currentTimer.initialize(totalTimeMs);
        
        // Hide quotes for circular timer to save space
        TextView lockscreenMessage = findViewById(R.id.lockscreenMessage);
        if (lockscreenMessage != null) {
            if ("circular".equals(timerStyle)) {
                lockscreenMessage.setVisibility(View.GONE);
            } else {
                // Digital timer: check if quotes are enabled
                boolean quotesEnabled = preferences.getBoolean("show_quotes", true);
                lockscreenMessage.setVisibility(quotesEnabled ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    private void startCountdownTimer(long totalTimeMs, long remainingTimeMillis) {
        countDownTimer = new android.os.CountDownTimer(remainingTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (currentTimer != null) {
                    currentTimer.updateTimer(totalTimeMs, millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
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

                    Toast.makeText(LockScreenActivity.this, "Time's up! Focus Mode Ended.", Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        };
        countDownTimer.start();
    }


}
