package com.grepguru.zenlock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
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
    private Button exitButton; // New exit button
    
    private CardView accessibilityCard;
    private CardView overlayCard;
    private CardView usageCard;
    private CardView alarmCard;
    
    // Accessibility consent tracking
    private boolean hasShownAccessibilityDisclosure = false;
    private boolean userConsentedToAccessibility = false;
    
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
        exitButton = findViewById(R.id.exitButton); // Initialize exit button
        
        accessibilityCard = findViewById(R.id.accessibilityCard);
        overlayCard = findViewById(R.id.overlayCard);
        usageCard = findViewById(R.id.usageCard);
        alarmCard = findViewById(R.id.alarmCard);
    }
    
    private void setupClickListeners() {
        accessibilityButton.setOnClickListener(v -> showAccessibilityDisclosure());
        overlayButton.setOnClickListener(v -> requestOverlayPermission());
        usageButton.setOnClickListener(v -> requestUsagePermission());
        alarmButton.setOnClickListener(v -> requestAlarmPermission());
        continueButton.setOnClickListener(v -> completeOnboarding());
        exitButton.setOnClickListener(v -> finish()); // Set click listener for exit
    }
    
    private void showAccessibilityDisclosure() {
        // Show disclosure dialog first
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_accessibility_disclosure, null);
        
        Button agreeButton = dialogView.findViewById(R.id.agreeButton);
        Button declineButton = dialogView.findViewById(R.id.declineButton);
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        agreeButton.setOnClickListener(v -> {
            userConsentedToAccessibility = true;
            hasShownAccessibilityDisclosure = true;
            dialog.dismiss();
            requestAccessibilityPermission();
        });
        
        declineButton.setOnClickListener(v -> {
            userConsentedToAccessibility = false;
            hasShownAccessibilityDisclosure = true;
            dialog.dismiss();
            Toast.makeText(this, "Accessibility permission is required for ZenLock to function properly", Toast.LENGTH_LONG).show();
        });
        
        // Prevent dismissing by tapping outside or back button
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
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
        
        // Update accessibility card based on consent and permission status
        if (hasShownAccessibilityDisclosure && !userConsentedToAccessibility) {
            // User declined consent - show declined state
            updatePermissionCard(accessibilityCard, accessibilityButton, false, "Declined", "Grant");
            accessibilityButton.setBackgroundResource(R.drawable.button_error);
        } else {
            updatePermissionCard(accessibilityCard, accessibilityButton, accessibilityGranted, "Granted", "Grant");
        }
        
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
        // AND user has consented to accessibility (if they declined, they can't continue)
        boolean essentialGranted = accessibilityGranted && overlayGranted && 
                                 (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmGranted) &&
                                 (hasShownAccessibilityDisclosure ? userConsentedToAccessibility : true);
        continueButton.setEnabled(essentialGranted);
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
            java.util.List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (android.accessibilityservice.AccessibilityServiceInfo service : enabledServices) {
                String serviceId = service.getId();
                if (serviceId.contains(getPackageName())) {
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
            // Check accessibility - use same logic as instance method
            boolean accessibilityGranted = false;
            android.view.accessibility.AccessibilityManager am =
                (android.view.accessibility.AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
            if (am != null) {
                String packageName = context.getPackageName();
                java.util.List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices =
                    am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                for (android.accessibilityservice.AccessibilityServiceInfo service : enabledServices) {
                    String serviceId = service.getId();
                    if (serviceId.startsWith(packageName + "/") ||
                        serviceId.contains("com.grepguru.zenlock.AppBlockerService")) {
                        accessibilityGranted = true;
                        break;
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
                if (alarmManager != null) {
                    alarmGranted = alarmManager.canScheduleExactAlarms();
                } else {
                    alarmGranted = false;
                }
            }
            // Final result
            return accessibilityGranted && overlayGranted && alarmGranted;
        } catch (Exception e) {
            return false; // If there's an error, assume permissions are not granted
        }
    }
}
