package com.grepguru.zenlock.permissions;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.grepguru.zenlock.R;

/** Consent is requested at the settings entry point, independently of feature setup. */
public class AccessibilityDisclosureDialog extends DialogFragment {
    private static final String TAG = "AccessibilityDisclosure";

    public static void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(TAG) != null) return;
        new AccessibilityDisclosureDialog().showNow(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_message)
                .setPositiveButton(R.string.accessibility_disclosure_agree, (dialog, which) ->
                        AppPermission.open(requireActivity(),
                                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                                getString(R.string.accessibility_settings_hint)))
                .setNegativeButton(R.string.accessibility_disclosure_decline, (dialog, which) -> dismiss())
                .create();
    }
}
