package com.grepguru.zenlock;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private android.view.View ambientMark;
    private final android.os.Handler ambientHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.Random ambientRandom = new java.util.Random();
    private final Runnable ambientBreath = this::breatheAmbientMark;
    private static final float AMBIENT_ALPHA = 0.09f;
    private float touchDownX;
    private float touchDownY;
    private final Runnable reviewCheck = () -> com.grepguru.zenlock.utils.ReviewPrompter.showIfDue(this);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (WelcomeActivity.shouldShow(this)) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        applyContentInsets();

        setupPermissionLauncher();
        cleanupStaleSessionState();
        activateEnabledSchedules();
        initializeAnalytics();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        ambientMark = findViewById(R.id.ambientMark);

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
    
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            touchDownX = event.getRawX();
            touchDownY = event.getRawY();
        } else if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP && ambientMark != null) {
            float slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
            boolean tap = Math.abs(event.getRawX() - touchDownX) < slop && Math.abs(event.getRawY() - touchDownY) < slop;
            if (tap && isOverAmbientMark(event.getRawX(), event.getRawY())) glowAmbientMark(0.4f, 450, 1500);
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean isOverAmbientMark(float rawX, float rawY) {
        int[] location = new int[2];
        ambientMark.getLocationOnScreen(location);
        float insetX = ambientMark.getWidth() * 0.22f;
        float insetY = ambientMark.getHeight() * 0.18f;
        return rawX > location[0] + insetX && rawX < location[0] + ambientMark.getWidth() - insetX
                && rawY > location[1] + insetY && rawY < location[1] + ambientMark.getHeight() - insetY;
    }

    private void scheduleAmbientBreath() {
        ambientHandler.removeCallbacks(ambientBreath);
        ambientHandler.postDelayed(ambientBreath, 6000 + ambientRandom.nextInt(14000));
    }

    private void breatheAmbientMark() {
        glowAmbientMark(0.18f + ambientRandom.nextFloat() * 0.08f, 1800, 2600);
        scheduleAmbientBreath();
    }

    private void glowAmbientMark(float peak, long riseMs, long fallMs) {
        if (ambientMark == null) return;
        ambientMark.animate().cancel();
        ambientMark.animate().alpha(peak).setDuration(riseMs)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> ambientMark.animate().alpha(AMBIENT_ALPHA).setDuration(fallMs)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).start())
                .start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ambientHandler.removeCallbacksAndMessages(null);
        if (ambientMark != null) {
            ambientMark.animate().cancel();
            ambientMark.setAlpha(AMBIENT_ALPHA);
        }
    }

    private void applyContentInsets() {
        android.view.View content = findViewById(R.id.fragmentContainer);
        int topGutter = Math.round(12 * getResources().getDisplayMetrics().density);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            androidx.core.graphics.Insets safeArea = windowInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safeArea.left, safeArea.top + topGutter, safeArea.right, 0);
            // Preserve dispatch to BottomNavigationView, including on Android 9 and 10.
            return windowInsets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(content);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (ambientMark != null) scheduleAmbientBreath();
        // Enforce lock: if locked, redirect to lock screen and prevent access
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);
        if (isLocked) {
            Intent lockIntent = new Intent(this, com.grepguru.zenlock.LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
            finish();
            return;
        }
        ambientHandler.postDelayed(reviewCheck, 2000);
    }
}
