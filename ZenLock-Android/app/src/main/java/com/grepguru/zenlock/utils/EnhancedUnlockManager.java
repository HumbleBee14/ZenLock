package com.grepguru.zenlock.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.guards.PinUnlock;
import com.grepguru.zenlock.model.UnlockMethod;

public class EnhancedUnlockManager {

    private static final int SMS_PERMISSION_REQUEST = 1000;
    private static final long RESEND_DELAY_MS = 20_000L;

    private final Context context;
    private final OTPManager otpManager;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private OnUnlockListener unlockListener;
    private Dialog currentDialog;
    private UnlockMethod method = UnlockMethod.PIN_UNLOCK;

    private TextView hint;
    private View partnerRow;
    private MaterialButton sendCodeButton;
    private TextView sendStatus;
    private EditText pinInput;
    private TextView errorText;

    public interface OnUnlockListener {
        void onUnlockSuccess(UnlockMethod method);
        void onUnlockCancelled();
    }

    public EnhancedUnlockManager(Context context) {
        this.context = context;
        this.otpManager = new OTPManager(context);
    }

    public void setOnUnlockListener(OnUnlockListener listener) {
        this.unlockListener = listener;
    }

    public void showUnlockDialog() {
        if (!(context instanceof Activity)) return;
        cleanup();

        Context themed = new ContextThemeWrapper(context, R.style.Theme_ZenLock);
        View view = LayoutInflater.from(themed).inflate(R.layout.dialog_unlock, null);
        hint = view.findViewById(R.id.methodHint);
        partnerRow = view.findViewById(R.id.partnerRow);
        sendCodeButton = view.findViewById(R.id.sendCodeButton);
        sendStatus = view.findViewById(R.id.sendStatus);
        pinInput = view.findViewById(R.id.pinInput);
        errorText = view.findViewById(R.id.errorText);
        View chips = view.findViewById(R.id.methodChips);
        Chip pinChip = view.findViewById(R.id.pinChip);
        Chip partnerChip = view.findViewById(R.id.partnerChip);
        View codeField = view.findViewById(R.id.codeField);
        MaterialButton unlockButton = view.findViewById(R.id.unlockButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);

        boolean hasPin = PinUnlock.isEnabled(context);
        boolean hasPartner = otpManager.isSmsConfigured();

        currentDialog = new Dialog(themed);
        currentDialog.setContentView(view);
        currentDialog.setCancelable(false);
        com.grepguru.zenlock.ui.Popups.size(currentDialog);

        if (!hasPin && !hasPartner) {
            hint.setText("No unlock method set. Add a PIN or a partner in Settings.");
            chips.setVisibility(View.GONE);
            codeField.setVisibility(View.GONE);
            unlockButton.setVisibility(View.GONE);
            cancelButton.setText("Close");
        } else {
            chips.setVisibility(hasPin && hasPartner ? View.VISIBLE : View.GONE);
            pinChip.setOnClickListener(v -> selectMethod(UnlockMethod.PIN_UNLOCK, pinChip, partnerChip));
            partnerChip.setOnClickListener(v -> selectMethod(UnlockMethod.ACCOUNTABILITY_PARTNER_OTP, pinChip, partnerChip));
            selectMethod(hasPin ? UnlockMethod.PIN_UNLOCK : UnlockMethod.ACCOUNTABILITY_PARTNER_OTP, pinChip, partnerChip);
        }

        setupVisibilityToggle(view.findViewById(R.id.pinVisibilityToggle));
        pinInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { errorText.setVisibility(View.GONE); }
            @Override public void afterTextChanged(Editable s) {}
        });
        pinInput.setOnEditorActionListener((v, actionId, event) -> {
            unlockButton.performClick();
            return true;
        });
        sendCodeButton.setOnClickListener(v -> sendCode());

        cancelButton.setOnClickListener(v -> {
            cleanup();
            if (unlockListener != null) unlockListener.onUnlockCancelled();
        });
        unlockButton.setOnClickListener(v -> {
            String entered = pinInput.getText().toString().trim();
            if (!validate(entered)) return;
            UnlockMethod used = method;
            cleanup();
            if (unlockListener != null) unlockListener.onUnlockSuccess(used);
        });

        currentDialog.show();
    }

    private void selectMethod(UnlockMethod selected, Chip pinChip, Chip partnerChip) {
        method = selected;
        boolean partner = selected == UnlockMethod.ACCOUNTABILITY_PARTNER_OTP;
        pinChip.setChecked(!partner);
        partnerChip.setChecked(partner);
        partnerRow.setVisibility(partner ? View.VISIBLE : View.GONE);
        errorText.setVisibility(View.GONE);
        pinInput.setText("");
        pinInput.setHint(partner ? "Code" : "••••");
        if (!partner) {
            hint.setText("Enter your PIN.");
            return;
        }
        hint.setText("Your partner gets a code by SMS. Enter it here.");
        refreshSendState();
    }

    private void refreshSendState() {
        if (!otpManager.hasSmsPermission()) {
            sendCodeButton.setText("Allow SMS");
            sendCodeButton.setEnabled(true);
            sendStatus.setText("SMS permission needed");
        } else if (otpManager.hasValidOTP()) {
            sendCodeButton.setText("Send again");
            sendCodeButton.setEnabled(true);
            sendStatus.setText("Code sent · valid " + remainingMinutes() + " min");
        } else {
            sendCodeButton.setText("Send code");
            sendCodeButton.setEnabled(true);
            sendStatus.setText("Valid for 5 minutes");
        }
    }

    private void sendCode() {
        if (!otpManager.hasSmsPermission()) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST);
            return;
        }
        sendCodeButton.setEnabled(false);
        sendCodeButton.setText("Sending");
        boolean sent = otpManager.requestOTPFromPartner();
        if (sent) {
            sendCodeButton.setText("Sent");
            sendStatus.setText("Code sent · valid 5 min");
            handler.postDelayed(() -> {
                if (currentDialog != null && currentDialog.isShowing()) refreshSendState();
            }, RESEND_DELAY_MS);
        } else {
            sendCodeButton.setEnabled(true);
            sendCodeButton.setText("Send code");
            sendStatus.setText("Could not send. Check partner settings.");
        }
    }

    private String remainingMinutes() {
        long remaining = otpManager.getRemainingOTPTime();
        return String.valueOf(Math.max(1, (remaining + 59_000) / 60_000));
    }

    private boolean validate(String entered) {
        if (entered.length() != 4) return fail("Enter the 4 digits.");
        if (method == UnlockMethod.PIN_UNLOCK) {
            String pin = PinUnlock.activePin(context);
            if (pin.isEmpty()) return fail("PIN not set.");
            return pin.equals(entered) || fail("Wrong PIN.");
        }
        return otpManager.verifyOTP(entered) || fail("Wrong or expired code.");
    }

    private boolean fail(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        return false;
    }

    private void setupVisibilityToggle(ImageView toggle) {
        toggle.setOnClickListener(v -> {
            boolean hidden = pinInput.getInputType() == (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pinInput.setInputType(hidden ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            toggle.setImageResource(hidden ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            pinInput.setSelection(pinInput.getText().length());
        });
    }

    public void cleanup() {
        handler.removeCallbacksAndMessages(null);
        if (currentDialog != null && currentDialog.isShowing()) currentDialog.dismiss();
        currentDialog = null;
    }
}
