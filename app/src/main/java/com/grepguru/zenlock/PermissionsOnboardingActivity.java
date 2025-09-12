package com.grepguru.zenlock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.grepguru.zenlock.utils.AlarmPermissionManager;

/**
 * Permissions Onboarding Activity
 * Professional screen to request essential permissions for new users
 * Blocks access to main app until all critical permissions are granted
 */
public class PermissionsOnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "PermissionsOnboarding";
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_PERMISSIONS_COMPLETED = "permissions_onboarding_completed";
    
    private Button accessibilityButton;
    private Button overlayButton;
    private Button usageButton;
    private Button alarmButton;
    private Button continueButton;
    
    private CardView accessibilityCard;
    private CardView overlayCard;
    private CardView usageCard;
    private CardView alarmCard;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_onboarding);
        
        initializeViews();
        setupClickListeners();
        updatePermissionStates();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStates();
    }
    
    private void initializeViews() {
        accessibilityButton = findViewById(R.id.accessibilityButton);
        overlayButton = findViewById(R.id.overlayButton);
        usageButton = findViewById(R.id.usageButton);
        alarmButton = findViewById(R.id.alarmButton);
        continueButton = findViewById(R.id.continueButton);
        
        accessibilityCard = findViewById(R.id.accessibilityCard);
        overlayCard = findViewById(R.id.overlayCard);
        usageCard = findViewById(R.id.usageCard);
        alarmCard = findViewById(R.id.alarmCard);
    }
    
    private void setupClickListeners() {
        accessibilityButton.setOnClickListener(v -> requestAccessibilityPermission());
        overlayButton.setOnClickListener(v -> requestOverlayPermission());
        usageButton.setOnClickListener(v -> requestUsagePermission());
        alarmButton.setOnClickListener(v -> requestAlarmPermission());
        continueButton.setOnClickListener(v -> completeOnboarding());
    }
    
    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Please enable Accessibility for ZenLock", Toast.LENGTH_LONG).show();
    }
    
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        Toast.makeText(this, "Please enable 'Display over other apps' for ZenLock", Toast.LENGTH_LONG).show();
    }
    
    private void requestUsagePermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Please enable 'Usage access' for ZenLock", Toast.LENGTH_LONG).show();
    }
    
    private void requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
            Toast.makeText(this, "Please enable 'Exact alarms' for ZenLock", Toast.LENGTH_LONG).show();
        }
    }
    
    private void updatePermissionStates() {
        boolean accessibilityGranted = isAccessibilityPermissionGranted();
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean usageGranted = isUsagePermissionGranted();
        boolean alarmGranted = isAlarmPermissionGranted();
        
        updatePermissionCard(accessibilityCard, accessibilityButton, accessibilityGranted, "Granted", "Grant");
        updatePermissionCard(overlayCard, overlayButton, overlayGranted, "Granted", "Grant");
        updatePermissionCard(usageCard, usageButton, usageGranted, "Granted", "Grant");
        
        // Only show alarm permission for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updatePermissionCard(alarmCard, alarmButton, alarmGranted, "Granted", "Grant");
            alarmCard.setVisibility(View.VISIBLE);
        } else {
            alarmCard.setVisibility(View.GONE);
        }
        
        // Enable continue button only if essential permissions are granted (Usage Access is optional)
        boolean essentialGranted = accessibilityGranted && overlayGranted && 
                                 (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmGranted);
        continueButton.setEnabled(essentialGranted);
        
        Log.d(TAG, "Permissions - Accessibility: " + accessibilityGranted + 
                   ", Overlay: " + overlayGranted + 
                   ", Usage: " + usageGranted +
                   ", Alarm: " + alarmGranted);
    }
    
    private void updatePermissionCard(CardView card, Button button, boolean granted, 
                                    String grantedText, String grantText) {
        if (granted) {
            button.setText(grantedText);
            button.setBackgroundResource(R.drawable.button_success);
            button.setEnabled(false);
            card.setAlpha(0.7f);
        } else {
            button.setText(grantText);
            button.setBackgroundResource(R.drawable.modern_button_primary);
            button.setEnabled(true);
            card.setAlpha(1.0f);
        }
    }
    
    private boolean isAccessibilityPermissionGranted() {
        android.view.accessibility.AccessibilityManager am = 
            (android.view.accessibility.AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (android.accessibilityservice.AccessibilityServiceInfo service : 
                 am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (service.getId().contains(getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isUsagePermissionGranted() {
        // Use the same method as AnalyticsFragment for consistency
        return com.grepguru.zenlock.utils.UsageStatsPermissionManager.hasUsageStatsPermission(this);
    }
    
    private boolean isAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = 
                (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Not required for older versions
    }
    
    private void completeOnboarding() {
        // Mark onboarding as completed
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PERMISSIONS_COMPLETED, true).apply();
        
        Log.d(TAG, "Permissions onboarding completed");
        
        // Navigate to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Check if permissions onboarding has been completed
     */
    public static boolean isOnboardingCompleted(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_PERMISSIONS_COMPLETED, false);
    }
    
    /**
     * Check if all essential permissions are granted
     */
    public static boolean areEssentialPermissionsGranted(android.content.Context context) {
        // Check accessibility with permission memory system (same as HomeFragment)
        android.view.accessibility.AccessibilityManager am = 
            (android.view.accessibility.AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        boolean accessibilityGranted = false;
        if (am != null) {
            // Check if accessibility is enabled globally
            if (!am.isEnabled()) {
                accessibilityGranted = false;
            } else {
                // Check if our service is in the enabled services list
                for (android.accessibilityservice.AccessibilityServiceInfo service : 
                     am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                    if (service.getId().contains(context.getPackageName())) {
                        // Mark permission as granted when we find the service is enabled
                        android.content.SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", android.content.Context.MODE_PRIVATE);
                        prefs.edit().putBoolean("accessibility_permission_granted", true).apply();
                        accessibilityGranted = true;
                        break;
                    }
                }
                // If accessibility is enabled but our service isn't in the list,
                // check if the user has previously granted permission (stored in SharedPreferences)
                if (!accessibilityGranted) {
                    android.content.SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", android.content.Context.MODE_PRIVATE);
                    accessibilityGranted = prefs.getBoolean("accessibility_permission_granted", false);
                }
            }
        }
        
        // Check overlay
        boolean overlayGranted = Settings.canDrawOverlays(context);
        
        // Check alarm (API 31+)
        boolean alarmGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = 
                (android.app.AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmGranted = alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        
        // Only check essential permissions (Usage Access is optional)
        return accessibilityGranted && overlayGranted && alarmGranted;
    }
}
