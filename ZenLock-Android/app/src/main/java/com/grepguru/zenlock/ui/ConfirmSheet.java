package com.grepguru.zenlock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.grepguru.zenlock.R;

public class ConfirmSheet extends BottomSheetDialogFragment {

    private static final String TAG = "ConfirmSheet";

    private String title;
    private String message;
    private String cancelLabel;
    private String actionLabel;
    private Runnable onConfirm;

    public static void show(FragmentActivity activity, String title, String message,
                            String cancelLabel, String actionLabel, Runnable onConfirm) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null) return;
        ConfirmSheet sheet = new ConfirmSheet();
        sheet.title = title;
        sheet.message = message;
        sheet.cancelLabel = cancelLabel;
        sheet.actionLabel = actionLabel;
        sheet.onConfirm = onConfirm;
        sheet.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (onConfirm == null) dismissAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_confirm, container, false);
        ((TextView) view.findViewById(R.id.confirmTitle)).setText(title);
        ((TextView) view.findViewById(R.id.confirmMessage)).setText(message);
        MaterialButton cancel = view.findViewById(R.id.confirmCancel);
        MaterialButton action = view.findViewById(R.id.confirmAction);
        cancel.setText(cancelLabel);
        action.setText(actionLabel);
        cancel.setOnClickListener(v -> dismiss());
        action.setOnClickListener(v -> {
            Runnable confirm = onConfirm;
            dismiss();
            if (confirm != null) confirm.run();
        });
        return view;
    }
}
