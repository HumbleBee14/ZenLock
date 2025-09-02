package com.grepguru.zenlock;

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

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup permission launcher
        setupPermissionLauncher();
        
        // Check and request notification permission
        checkNotificationPermission();

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
}
