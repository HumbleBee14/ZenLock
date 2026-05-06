package com.grepguru.zenlock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.grepguru.zenlock.utils.MiuiUtils;

/**
 * Permissions Onboarding Activity
 * Professional screen to request essential permissions for new users
 * Blocks access to main app until all critical permissions are granted
 */
public class PermissionsOnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "PermissionsOnboarding";
    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";

    private Button accessibilityButton;
    private Button overlayButton;
    private Button alarmButton;
    private Button miuiButton;
    private Button continueButton;
    private Button skipButton;

    private CardView accessibilityCard;
    private CardView overlayCard;
    private CardView alarmCard;
    private CardView miuiCard;
    
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
    }
    
    private void initializeViews() {
        accessibilityButton = findViewById(R.id.accessibilityButton);
        overlayButton = findViewById(R.id.overlayButton);
        alarmButton = findViewById(R.id.alarmButton);
        miuiButton = findViewById(R.id.miuiButton);
        continueButton = findViewById(R.id.continueButton);
        skipButton = findViewById(R.id.skipButton);

        accessibilityCard = findViewById(R.id.accessibilityCard);
        overlayCard = findViewById(R.id.overlayCard);
        alarmCard = findViewById(R.id.alarmCard);
        miuiCard = findViewById(R.id.miuiCard);
    }
    
    private void setupClickListeners() {
        accessibilityButton.setOnClickListener(v -> showAccessibilityDisclosure());
        overlayButton.setOnClickListener(v -> requestOverlayPermission());
        alarmButton.setOnClickListener(v -> requestAlarmPermission());
        miuiButton.setOnClickListener(v -> requestMiuiPermission());
        continueButton.setOnClickListener(v -> completeOnboarding());
        skipButton.setOnClickListener(v -> completeOnboarding());
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
            Toast.makeText(this, "No problem — you can enable this later when you start a focus session.", Toast.LENGTH_LONG).show();
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
    
    private void requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
            Toast.makeText(this, "Please enable 'Exact alarms' for ZenLock", Toast.LENGTH_LONG).show();
        }
    }

    private void requestMiuiPermission() {
        MiuiUtils.openMiuiPermissionEditor(this);
        Toast.makeText(this, "Enable 'Display pop-up windows while running in background' for ZenLock", Toast.LENGTH_LONG).show();
    }
    
    private void updatePermissionStates() {
        boolean accessibilityGranted = isAccessibilityPermissionGranted();
        boolean overlayGranted = Settings.canDrawOverlays(this);
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
        // Only show alarm permission for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updatePermissionCard(alarmCard, alarmButton, alarmGranted, "Granted", "Grant");
            alarmCard.setVisibility(View.VISIBLE);
        } else {
            alarmCard.setVisibility(View.GONE);
        }

        // Only show MIUI card on Xiaomi/Redmi/POCO devices
        if (MiuiUtils.isXiaomiDevice()) {
            boolean miuiGranted = MiuiUtils.canStartActivityFromBackground(this);
            updatePermissionCard(miuiCard, miuiButton, miuiGranted, "Granted", "Grant");
            miuiCard.setVisibility(View.VISIBLE);
        } else {
            miuiCard.setVisibility(View.GONE);
        }

        // Show one action: "Continue" if all essential granted, else "Skip for now".
        boolean essentialGranted = accessibilityGranted && overlayGranted &&
                                 (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmGranted);
        continueButton.setVisibility(essentialGranted ? View.VISIBLE : View.GONE);
        skipButton.setVisibility(essentialGranted ? View.GONE : View.VISIBLE);
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
    
    private boolean isAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = 
                (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Not required for older versions
    }
    
    private void completeOnboarding() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static boolean hasSeenOnboarding(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_SEEN, false);
    }
}
