package com.grepguru.focuslock.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.grepguru.focuslock.R;
import com.grepguru.focuslock.model.UnlockMethod;

/**
 * Enhanced unlock manager that handles multiple unlock methods
 * including basic PIN, trusted person PIN, and accountability partner OTP
 */
public class EnhancedUnlockManager {
    
    private static final String TAG = "EnhancedUnlockManager";
    private static final String PREFS_NAME = "FocusLockPrefs";
    
    private Context context;
    private SharedPreferences preferences;
    private OTPManager otpManager;
    private OnUnlockListener unlockListener;
    private AlertDialog currentDialog;
    private CountDownTimer otpTimer;
    
    public interface OnUnlockListener {
        void onUnlockSuccess(UnlockMethod method);
        void onUnlockCancelled();
    }
    
    public EnhancedUnlockManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.otpManager = new OTPManager(context);
    }
    
    public void setOnUnlockListener(OnUnlockListener listener) {
        this.unlockListener = listener;
    }
    
    /**
     * Shows the unlock options dialog
     */
    public void showUnlockDialog() {
        if (!(context instanceof Activity)) {
            Toast.makeText(context, "Unlock not available in this context", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Activity activity = (Activity) context;
        
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_unlock_options, null);
        
        // Get UI elements
        CardView pinUnlockCard = dialogView.findViewById(R.id.pinUnlockCard);
        CardView accountabilityPartnerCard = dialogView.findViewById(R.id.accountabilityPartnerCard);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        TextView pinStatus = dialogView.findViewById(R.id.pinStatus);
        TextView partnerStatus = dialogView.findViewById(R.id.partnerStatus);
        TextView otpStatus = dialogView.findViewById(R.id.otpStatus);
        
        // Update status texts
        updateUnlockOptionsStatus(pinStatus, partnerStatus, otpStatus);
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.ModernAlertDialog);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        currentDialog = builder.create();
        
        // Set up click listeners
        pinUnlockCard.setOnClickListener(v -> {
            if (isPinConfigured()) {
                currentDialog.dismiss();
                showPinInputDialog(UnlockMethod.PIN_UNLOCK);
            } else {
                Toast.makeText(context, "PIN not configured. Please set a PIN in settings.", Toast.LENGTH_SHORT).show();
            }
        });
        
        accountabilityPartnerCard.setOnClickListener(v -> {
            if (isAccountabilityPartnerConfigured()) {
                currentDialog.dismiss();
                showPinInputDialog(UnlockMethod.ACCOUNTABILITY_PARTNER_OTP);
            } else {
                Toast.makeText(context, "Accountability partner not configured", Toast.LENGTH_SHORT).show();
            }
        });
        
        cancelButton.setOnClickListener(v -> {
            currentDialog.dismiss();
            if (unlockListener != null) {
                unlockListener.onUnlockCancelled();
            }
        });
        
        currentDialog.show();
    }
    
    /**
     * Shows PIN input dialog for the selected unlock method
     */
    private void showPinInputDialog(UnlockMethod method) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_pin_input, null);
        
        // Get UI elements
        TextView titleText = dialogView.findViewById(R.id.unlockMethodTitle);
        TextView descriptionText = dialogView.findViewById(R.id.unlockMethodDescription);
        LinearLayout otpRequestSection = dialogView.findViewById(R.id.otpRequestSection);
        Button requestOtpButton = dialogView.findViewById(R.id.requestOtpButton);
        Button sendOtpAgainButton = dialogView.findViewById(R.id.sendOtpAgainButton);
        TextView otpStatusText = dialogView.findViewById(R.id.otpStatusText);
        EditText pinInput = dialogView.findViewById(R.id.pinInput);
        ImageView pinVisibilityToggle = dialogView.findViewById(R.id.pinVisibilityToggle);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        
        // Configure dialog based on unlock method
        configureDialogForMethod(method, titleText, descriptionText, otpRequestSection, 
                               requestOtpButton, sendOtpAgainButton, otpStatusText);
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.ModernAlertDialog);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        currentDialog = builder.create();
        
        // Set up PIN visibility toggle
        setupPinVisibilityToggle(pinInput, pinVisibilityToggle);
        
        // Set up PIN input text watcher to reset error state
        setupPinInputWatcher(pinInput, otpStatusText, method);
        
        // Set up OTP request (for accountability partner)
        if (method == UnlockMethod.ACCOUNTABILITY_PARTNER_OTP) {
            setupOTPRequest(requestOtpButton, sendOtpAgainButton, otpStatusText);
        }
        
        // Set up action buttons
        cancelButton.setOnClickListener(v -> {
            cleanupTimers();
            currentDialog.dismiss();
            showUnlockDialog(); // Go back to options
        });
        
        confirmButton.setOnClickListener(v -> {
            String enteredPin = pinInput.getText().toString().trim();
            if (validatePin(enteredPin, method, pinInput, otpStatusText)) {
                cleanupTimers();
                currentDialog.dismiss();
                if (unlockListener != null) {
                    unlockListener.onUnlockSuccess(method);
                }
            }
        });
        
        currentDialog.show();
    }
    
    private void configureDialogForMethod(UnlockMethod method, TextView titleText, 
                                        TextView descriptionText, LinearLayout otpRequestSection,
                                        Button requestOtpButton, Button sendOtpAgainButton, TextView otpStatusText) {
        switch (method) {
            case PIN_UNLOCK:
                titleText.setText("PIN Unlock");
                descriptionText.setText("Enter your configured PIN");
                otpRequestSection.setVisibility(View.GONE);
                // Initialize status text for PIN unlock error messages
                otpStatusText.setText("");
                otpStatusText.setVisibility(View.GONE); // Hidden by default, shown on error
                break;
                
            case ACCOUNTABILITY_PARTNER_OTP:
                titleText.setText("Partner Unlock");
                descriptionText.setText("Send unlock code to your accountability partner, then enter the code they receive");
                otpRequestSection.setVisibility(View.VISIBLE);
                
                // Keep it simple - just one button
                requestOtpButton.setText("Send Unlock Code");
                requestOtpButton.setEnabled(true);
                sendOtpAgainButton.setVisibility(View.GONE);
                otpStatusText.setText("Code valid for 1 hour only");
                otpStatusText.setVisibility(View.VISIBLE);
                break;
        }
    }
    
    private void setupOTPRequest(Button requestOtpButton, Button sendOtpAgainButton, TextView otpStatusText) {
        // Simple send unlock code button
        requestOtpButton.setOnClickListener(v -> {
            if (otpManager.requestOTPFromPartner()) {
                otpStatusText.setText("✓ Unlock code sent successfully!");
                otpStatusText.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                Toast.makeText(context, "Unlock code sent to your partner", Toast.LENGTH_SHORT).show();
            } else {
                otpStatusText.setText("✗ Failed to send unlock code");
                otpStatusText.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }
        });
        
        // Hide the send again button - we don't need it
        sendOtpAgainButton.setVisibility(View.GONE);
    }
    
    private void setupPinInputWatcher(EditText pinInput, TextView statusText, UnlockMethod method) {
        pinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Reset PIN input to normal color when user types
                resetPinInputState(pinInput, statusText, method);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void resetPinInputState(EditText pinInput, TextView statusText, UnlockMethod method) {
        // Reset PIN input background to normal
        pinInput.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4B5563"))); // Normal gray
        
        // Clear or reset status text based on method
        String currentText = statusText.getText().toString();
        if (currentText.contains("Invalid") || currentText.contains("Incorrect") || currentText.contains("wrong") || 
            currentText.contains("Wrong") || currentText.contains("Check again") || currentText.contains("not configured")) {
            
            if (method == UnlockMethod.ACCOUNTABILITY_PARTNER_OTP) {
                statusText.setText("Code valid for 1 hour only");
                statusText.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            } else {
                // For PIN unlock, just hide the status text
                statusText.setText("");
                statusText.setVisibility(View.GONE);
            }
        }
    }
    
    private void showPinError(EditText pinInput, TextView statusText, String errorMessage) {
        // Make PIN input red
        pinInput.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red color
        
        // Show error in status text
        statusText.setText(errorMessage);
        statusText.setTextColor(Color.parseColor("#FF6B6B")); // Red color for error
        statusText.setVisibility(View.VISIBLE); // Make sure it's visible
    }
    
    private void setupPinVisibilityToggle(EditText pinInput, ImageView toggleButton) {
        toggleButton.setOnClickListener(v -> {
            if (pinInput.getInputType() == (android.text.InputType.TYPE_CLASS_NUMBER | 
                                         android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)) {
                // Show PIN
                pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                toggleButton.setImageResource(R.drawable.ic_eye_off);
            } else {
                // Hide PIN
                pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                toggleButton.setImageResource(R.drawable.ic_eye);
            }
            // Move cursor to end
            pinInput.setSelection(pinInput.getText().length());
        });
    }
    
    private boolean validatePin(String enteredPin, UnlockMethod method, EditText pinInput, TextView statusText) {
        if (enteredPin == null || enteredPin.length() != 4) {
            showPinError(pinInput, statusText, "Please enter a 4-digit PIN");
            return false;
        }
        
        switch (method) {
            case PIN_UNLOCK:
                String pin = preferences.getString("unlock_pin", "");
                if (pin.isEmpty()) {
                    showPinError(pinInput, statusText, "PIN not configured");
                    return false;
                }
                if (pin.equals(enteredPin)) {
                    return true;
                } else {
                    showPinError(pinInput, statusText, "Wrong PIN! Check again.");
                    return false;
                }
                
            case ACCOUNTABILITY_PARTNER_OTP:
                // Simple validation - let backend handle OTP logic
                if (otpManager.validateOTP(enteredPin)) {
                    return true;
                } else {
                    showPinError(pinInput, statusText, "Wrong unlock code! Check again.");
                    return false;
                }
                
            default:
                return false;
        }
    }
    
    private void updateUnlockOptionsStatus(TextView pinStatus, TextView partnerStatus, TextView otpStatus) {
        // Update PIN status - only show "Not configured" when not configured
        if (isPinConfigured()) {
            pinStatus.setVisibility(View.GONE); // Hide status when configured
        } else {
            pinStatus.setText("Not configured");
            pinStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            pinStatus.setVisibility(View.VISIBLE);
        }
        
        // Update accountability partner status - only show "Not configured" when not configured
        if (isAccountabilityPartnerConfigured()) {
            partnerStatus.setVisibility(View.GONE); // Hide status when configured
        } else {
            partnerStatus.setText("Not configured");
            partnerStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            partnerStatus.setVisibility(View.VISIBLE);
        }
        
        // Always hide OTP status - keep it simple
        otpStatus.setVisibility(View.GONE);
    }
    
    private boolean isPinConfigured() {
        String pin = preferences.getString("unlock_pin", "");
        return !pin.isEmpty();
    }
    
    private boolean isAccountabilityPartnerConfigured() {
        boolean smsEnabled = preferences.getBoolean("enable_sms_notifications", false);
        String partnerPhone = preferences.getString("partner_phone", "");
        return smsEnabled && !partnerPhone.isEmpty();
    }
    
    private void cleanupTimers() {
        if (otpTimer != null) {
            otpTimer.cancel();
            otpTimer = null;
        }
    }
    
    /**
     * Cleanup method to be called when manager is no longer needed
     */
    public void cleanup() {
        cleanupTimers();
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
}
