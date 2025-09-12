package com.grepguru.zenlock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.grepguru.zenlock.fragments.HomeFragment;
import com.grepguru.zenlock.fragments.*;
import com.grepguru.zenlock.utils.NotificationPermissionManager;
import com.grepguru.zenlock.utils.ScheduleActivator;
import com.grepguru.zenlock.utils.AlarmPermissionManager;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.utils.ForegroundServicePermissionManager;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "=== MainActivity onCreate START ===");
        Log.d(TAG, "Intent that started this activity: " + getIntent().toString());

        // Check if essential permissions are granted - if not, show onboarding
        Log.d(TAG, "About to check essential permissions...");
        boolean permissionsGranted = PermissionsOnboardingActivity.areEssentialPermissionsGranted(this);
        Log.d(TAG, "Essential permissions check result: " + permissionsGranted);

        if (!permissionsGranted) {
            Log.d(TAG, "Permissions not granted, redirecting to onboarding");
            // Redirect to permissions onboarding
            startActivity(new Intent(this, PermissionsOnboardingActivity.class));
            finish();
            return;
        }
        
        Log.d(TAG, "All permissions granted, continuing to main app");
        setContentView(R.layout.activity_main);
        
        // Setup permission launcher
        setupPermissionLauncher();
        
        // Check and request notification permission
        checkNotificationPermission();
        
                // Check exact alarm permission for schedules
                checkExactAlarmPermission();

                // Check foreground service permission for background launch
                checkForegroundServicePermission();

                // Clean up any stale session state
                cleanupStaleSessionState();
        
        // Ensure all enabled schedules are activated
        activateEnabledSchedules();
        
        // Initialize analytics and auto-fetch data
        initializeAnalytics();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new HomeFragment()).commit();
        }

        // Bottom Navigation Handling
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.settings) {
                selectedFragment = new SettingsFragment();
            } else if (id == R.id.analytics) {
                selectedFragment = new AnalyticsFragment();
            } else if (id == R.id.schedule) {
                selectedFragment = new ScheduleFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, selectedFragment).commit();
            }
            return true;
        });
    }
    
    /**
     * Setup permission request launcher
     */
    private void setupPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                    // Permission granted, notifications can now be sent
                } else {
                    Log.w(TAG, "Notification permission denied");
                    // Show settings dialog if permanently denied
                    if (NotificationPermissionManager.isPermissionPermanentlyDenied(this)) {
                        NotificationPermissionManager.showSettingsDialog(this);
                    }
                }
            }
        );
    }
    
    /**
     * Check and request notification permission if needed
     */
    private void checkNotificationPermission() {
        if (!NotificationPermissionManager.hasNotificationPermission(this)) {
            Log.d(TAG, "Requesting notification permission");
            NotificationPermissionManager.requestNotificationPermission(this, notificationPermissionLauncher);
        } else {
            Log.d(TAG, "Notification permission already granted");
        }
    }
    
    /**
     * Check exact alarm permission for scheduling
     */
    private void checkExactAlarmPermission() {
        if (!AlarmPermissionManager.canScheduleExactAlarms(this)) {
            Log.d(TAG, "Exact alarm permission not granted, requesting...");
            AlarmPermissionManager.requestExactAlarmPermission(this);
        } else {
            Log.d(TAG, "Exact alarm permission already granted");
        }
    }

    /**
     * Check foreground service permission for background launch
     */
    private void checkForegroundServicePermission() {
        if (!ForegroundServicePermissionManager.canStartForegroundService(this)) {
            Log.d(TAG, "Foreground service permission not granted, requesting...");
            ForegroundServicePermissionManager.requestForegroundServicePermission(this);
        } else {
            Log.d(TAG, "Foreground service permission already granted");
        }
    }
    
    /**
     * Clean up any stale session state that might prevent new sessions
     */
    private void cleanupStaleSessionState() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
            boolean isLocked = prefs.getBoolean("isLocked", false);
            long lockEndTime = prefs.getLong("lockEndTime", 0);
            long currentTime = System.currentTimeMillis();
            
                    if (isLocked && lockEndTime > 0 && currentTime >= lockEndTime) {
                        Log.w(TAG, "Found expired session on app start, cleaning up");
                        android.content.SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isLocked", false);
                        editor.remove("lockEndTime");
                        editor.remove("uptimeAtLock");
                        editor.remove("wasDeviceRestarted");
                        editor.remove("current_session_source");
                        editor.apply();
                    }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup stale session state", e);
        }
    }
    
    /**
     * Activate all enabled schedules on app start
     */
    private void activateEnabledSchedules() {
        try {
            Log.d(TAG, "Activating enabled schedules on app start");
            ScheduleActivator scheduleActivator = new ScheduleActivator(this);
            
            scheduleActivator.scheduleAllSchedules();
            Log.d(TAG, "Schedule activation process completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to activate schedules", e);
        }
    }
    
    /**
     * Initialize analytics and auto-fetch data on app start
     */
    private void initializeAnalytics() {
        try {
            Log.d(TAG, "Initializing analytics on app start");
            AnalyticsManager analyticsManager = new AnalyticsManager(this);
            
            // Auto-fetch mobile usage data if permission is available
            analyticsManager.updateTodayMobileUsageIfAvailable();
            Log.d(TAG, "Analytics initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize analytics", e);
        }
    }
}
