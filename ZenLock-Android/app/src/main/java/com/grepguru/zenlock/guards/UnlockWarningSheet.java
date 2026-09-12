package com.grepguru.zenlock.guards;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.grepguru.zenlock.R;

public class UnlockWarningSheet extends BottomSheetDialogFragment {

    private static final String TAG = "UnlockWarningSheet";

    private String actionLabel;
    private Runnable onProceed;

    static void show(FragmentActivity activity, String actionLabel, Runnable onProceed) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null) return;
        UnlockWarningSheet sheet = new UnlockWarningSheet();
        sheet.actionLabel = actionLabel;
        sheet.onProceed = onProceed;
        sheet.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (onProceed == null) dismissAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_unlock_warning, container, false);
        MaterialCheckBox dontAskAgain = view.findViewById(R.id.unlockWarningDontAsk);
        MaterialButton cancel = view.findViewById(R.id.unlockWarningCancel);
        MaterialButton proceed = view.findViewById(R.id.unlockWarningProceed);
        proceed.setText(actionLabel);
        cancel.setOnClickListener(v -> dismiss());
        proceed.setOnClickListener(v -> {
            UnlockMethodGuard.acknowledge(requireContext(), dontAskAgain.isChecked());
            Runnable action = onProceed;
            dismiss();
            if (action != null) action.run();
        });
        return view;
    }
}
