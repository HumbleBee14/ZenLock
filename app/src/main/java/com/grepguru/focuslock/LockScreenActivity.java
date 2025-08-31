package com.grepguru.focuslock;

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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.focuslock.model.*;
import com.grepguru.focuslock.ui.adapter.*;
import com.grepguru.focuslock.utils.AppUtils;
import com.grepguru.focuslock.utils.AnalyticsManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockScreenActivity extends AppCompatActivity {

    private EditText pinInput;
    private SharedPreferences preferences;
    private boolean isLaunchingWhitelistedApp = false;
    private AnalyticsManager analyticsManager;
    private boolean isExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        analyticsManager = new AnalyticsManager(this);

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
        LinearLayout unlockInputsContainer = findViewById(R.id.unlockInputsContainer);
        LinearLayout appsSection = findViewById(R.id.appsSection);

        // Start countdown timer with remaining time
        long remainingTimeMillis = lockEndTime - currentTime;
        if (remainingTimeMillis <= 0) {
            finish();
            return;
        }

        // Single RecyclerView for all apps (default + additional)
        RecyclerView appsRecycler = findViewById(R.id.defaultAppsRecycler);
        android.widget.ImageView expandAppsButton = findViewById(R.id.expandAppsButton);
        LinearLayout expandButtonContainer = findViewById(R.id.expandButtonContainer);

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
                
                // Show the RecyclerView
                appsRecycler.setVisibility(View.VISIBLE);
                
                // Scroll to show all apps after a short delay
                appsRecycler.postDelayed(() -> {
                    layoutManager.scrollToPosition(currentAppModels.size() - 1);
                }, 100);
            } else {
                // Collapse: Hide all apps
                currentAppModels.clear();
                appsAdapter.notifyDataSetChanged();
                
                // Hide the RecyclerView
                appsRecycler.setVisibility(View.GONE);
            }

            isExpanded = !isExpanded;

            // Rotate the expand icon (180° for expanded, 0° for collapsed)
            expandAppsButton.animate()
                .rotation(isExpanded ? 180 : 0)
                .setDuration(300)
                .start();
        });


        // -----------------------------------------------------------
        // Setting up Click Listeners

        // Initially Hide PIN Input and Keep Apps Visible
        unlockInputsContainer.setVisibility(View.GONE);
        appsSection.setVisibility(View.VISIBLE);

        unlockPromptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show PIN Input when unlocking and hide apps
                unlockInputsContainer.setVisibility(View.VISIBLE);
                unlockPromptButton.setVisibility(View.GONE);
                appsSection.setVisibility(View.GONE);

                // If additional apps were expanded, collapse them
                if (isExpanded) {
                    currentAppModels.removeAll(additionalAppModels);
                    appsAdapter.notifyDataSetChanged();
                    isExpanded = false;
                    expandAppsButton.setRotation(0);
                }
            }
        });

        unlockButton.setOnClickListener(v -> checkPinAndUnlock());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if unlock input screen is visible
                if (unlockInputsContainer.getVisibility() == View.VISIBLE) {
                    // Hide unlock inputs and show main lock screen
                    unlockInputsContainer.setVisibility(View.GONE);
                    unlockPromptButton.setVisibility(View.VISIBLE);
                    appsSection.setVisibility(View.VISIBLE);

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
        startCountdownTimer(remainingTimeMillis , timerCountdown);
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
            return;
        }
        
        // Restart LockScreenActivity if user tries to minimize
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    private void checkPinAndUnlock() {
        String enteredPin = pinInput.getText().toString();
        String savedPin = preferences.getString("lock_pin", "");

        if (enteredPin.equals(savedPin)) {
            Toast.makeText(this, "Unlocked!", Toast.LENGTH_SHORT).show();

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
            Intent intent = new Intent(LockScreenActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
        }
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
            "Don't watch the clock; do what it does. Keep going.",
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

    private void startCountdownTimer(long remainingTimeMillis, TextView timerCountdown) {
        new android.os.CountDownTimer(remainingTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                timerCountdown.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                // End analytics session
                if (analyticsManager.hasActiveSession()) {
                    analyticsManager.endSession(true); // Completed successfully
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLocked", false);
                editor.remove("lockEndTime"); // Remove saved lock end time
                editor.apply();

                Toast.makeText(LockScreenActivity.this, "Time's up! Focus Mode Ended.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.start();
    }


}
