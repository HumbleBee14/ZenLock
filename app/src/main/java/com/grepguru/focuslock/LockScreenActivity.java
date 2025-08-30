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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockScreenActivity extends AppCompatActivity {

    private EditText pinInput;
    private SharedPreferences preferences;
    private boolean isLaunchingWhitelistedApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);

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
        LinearLayout emergencyAppsContainer = findViewById(R.id.emergencyAppsContainer);

        // Start countdown timer with remaining time
        long remainingTimeMillis = lockEndTime - currentTime;
        if (remainingTimeMillis <= 0) {
            finish();
            return;
        }

        // Whitelisted Apps
        RecyclerView whitelistedAppsRecycler = findViewById(R.id.whitelistedAppsRecycler);
        whitelistedAppsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        Set<String> whitelistedApps = preferences.getStringSet("whitelisted_apps", new HashSet<>());

        // Add Default Apps Explicitly
        whitelistedApps.addAll(AppUtils.getDefaultApps(this));

        List<AppModel> allowedApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (String packageName : whitelistedApps) {
            try {
                Drawable icon = pm.getApplicationIcon(packageName);
                String appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
                allowedApps.add(new AppModel(packageName, appName, true, icon)); // Mark as default
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Set adapter
        AllowedAppsAdapter adapter = new AllowedAppsAdapter(this, allowedApps);
        adapter.setOnAppLaunchListener(() -> {
            isLaunchingWhitelistedApp = true;
        });
        whitelistedAppsRecycler.setAdapter(adapter);


        // -----------------------------------------------------------
        // Setting up Click Listeners

        // Initially Hide PIN Input and Keep Emergency Apps Visible
        unlockInputsContainer.setVisibility(View.GONE);
        emergencyAppsContainer.setVisibility(View.VISIBLE);

        unlockPromptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show PIN Input when unlocking and hide emergency buttons
                unlockInputsContainer.setVisibility(View.VISIBLE);
                unlockPromptButton.setVisibility(View.GONE);
                emergencyAppsContainer.setVisibility(View.GONE);
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
                    emergencyAppsContainer.setVisibility(View.VISIBLE);

                    pinInput.setText("");
                } else {
                    // Prevent exiting the app
                    Toast.makeText(LockScreenActivity.this, "Cannot exit Focus Mode!", Toast.LENGTH_SHORT).show();
                }
            }
        });

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
