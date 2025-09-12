package com.grepguru.zenlock;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Permissions Onboarding Activity
 * Professional screen to request essential permissions for new users
 * Blocks access to main app until all critical permissions are granted
 */
public class PermissionsOnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "PermissionsOnboarding";

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
        // Removed auto-navigation - let user click "Continue to App" button instead
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
            Log.d(TAG, "Instance method: Checking accessibility services...");
            java.util.List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            Log.d(TAG, "Instance method: Found " + enabledServices.size() + " enabled services");

            for (android.accessibilityservice.AccessibilityServiceInfo service : enabledServices) {
                String serviceId = service.getId();
                Log.d(TAG, "Instance method: Found service: " + serviceId);
                if (serviceId.contains(getPackageName())) {
                    Log.d(TAG, "Instance method: Found our service: " + serviceId);
                    return true;
                }
            }
            Log.d(TAG, "Instance method: Our package " + getPackageName() + " not found");
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
        Log.d(TAG, "Navigating to main app");

        // Navigate to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Check if all essential permissions are granted
     */
    public static boolean areEssentialPermissionsGranted(android.content.Context context) {
        try {
            Log.d("PermissionsCheck", "=== STARTING PERMISSION CHECK ===");

            // Check accessibility - use same logic as instance method
            boolean accessibilityGranted = false;
            android.view.accessibility.AccessibilityManager am =
                (android.view.accessibility.AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
            if (am != null) {
                String packageName = context.getPackageName();
                Log.d("PermissionsCheck", "Our package name: " + packageName);
                Log.d("PermissionsCheck", "Accessibility Manager available, checking enabled services...");
                java.util.List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices =
                    am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                Log.d("PermissionsCheck", "Found " + enabledServices.size() + " enabled accessibility services");

                for (android.accessibilityservice.AccessibilityServiceInfo service : enabledServices) {
                    String serviceId = service.getId();
                    Log.d("PermissionsCheck", "Found accessibility service: " + serviceId);

                    // Check if this service belongs to our app (handle debug package names)
                    if (serviceId.startsWith(packageName + "/") ||
                        serviceId.contains("com.grepguru.zenlock.AppBlockerService")) {
                        Log.d("PermissionsCheck", "✓ Found our accessibility service: " + serviceId);
                        accessibilityGranted = true;
                        break;
                    }
                }
                if (!accessibilityGranted) {
                    Log.d("PermissionsCheck", "✗ Our package name: " + packageName + " not found in any enabled service");
                }
            } else {
                Log.e("PermissionsCheck", "✗ AccessibilityManager is null!");
            }

            // Check overlay
            boolean overlayGranted = Settings.canDrawOverlays(context);
            Log.d("PermissionsCheck", "Overlay permission: " + (overlayGranted ? "✓ GRANTED" : "✗ DENIED"));

            // Check alarm (API 31+)
            boolean alarmGranted = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.AlarmManager alarmManager =
                    (android.app.AlarmManager) context.getSystemService(ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmGranted = alarmManager.canScheduleExactAlarms();
                    Log.d("PermissionsCheck", "Exact alarm permission (API " + Build.VERSION.SDK_INT + "): " + (alarmGranted ? "✓ GRANTED" : "✗ DENIED"));
                } else {
                    Log.e("PermissionsCheck", "✗ AlarmManager is null!");
                    alarmGranted = false;
                }
            } else {
                Log.d("PermissionsCheck", "Exact alarm permission not required for API " + Build.VERSION.SDK_INT);
            }

            // Final result
            boolean result = accessibilityGranted && overlayGranted && alarmGranted;
            Log.d("PermissionsCheck", "=== FINAL RESULT ===");
            Log.d("PermissionsCheck", "Accessibility: " + (accessibilityGranted ? "✓" : "✗"));
            Log.d("PermissionsCheck", "Overlay: " + (overlayGranted ? "✓" : "✗"));
            Log.d("PermissionsCheck", "Alarm: " + (alarmGranted ? "✓" : "✗"));
            Log.d("PermissionsCheck", "Overall result: " + (result ? "✓ ALL GRANTED" : "✗ MISSING PERMISSIONS"));
            Log.d("PermissionsCheck", "=== END PERMISSION CHECK ===");

            return result;
        } catch (Exception e) {
            Log.e("PermissionsCheck", "❌ EXCEPTION in permission check: " + e.getMessage(), e);
            return false; // If there's an error, assume permissions are not granted
        }
    }
}
