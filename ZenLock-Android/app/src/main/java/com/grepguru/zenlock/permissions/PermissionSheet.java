package com.grepguru.zenlock.permissions;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.grepguru.zenlock.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionSheet extends BottomSheetDialogFragment implements PermissionHost {

    private static final String TAG = "PermissionSheet";

    private PermissionRequest request;
    private Runnable onReady;
    private LinearLayout rows;
    private MaterialButton continueButton;

    private final ActivityResultLauncher<String> runtimePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> render());

    static void show(FragmentActivity activity, PermissionRequest request, @Nullable Runnable onReady) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null) return;
        PermissionSheet sheet = new PermissionSheet();
        sheet.request = request;
        sheet.onReady = onReady;
        sheet.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (request == null) dismissAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_permissions, container, false);
        TextView title = view.findViewById(R.id.permissionsTitle);
        rows = view.findViewById(R.id.permissionRows);
        continueButton = view.findViewById(R.id.permissionsContinue);
        if (request != null) title.setText(request.title);
        continueButton.setText(onReady == null ? "Done" : "Continue");
        continueButton.setOnClickListener(v -> {
            Runnable ready = onReady;
            dismiss();
            if (ready != null) ready.run();
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        if (request == null || rows == null || getContext() == null) return;
        List<PermissionRequest.Requirement> missing = new ArrayList<>();
        List<PermissionRequest.Requirement> optional = new ArrayList<>();
        List<PermissionRequest.Requirement> granted = new ArrayList<>();
        for (PermissionRequest.Requirement requirement : request.applicable(requireContext())) {
            if (requirement.permission.isGranted(requireContext())) granted.add(requirement);
            else if (requirement.required) missing.add(requirement);
            else optional.add(requirement);
        }
        rows.removeAllViews();
        for (PermissionRequest.Requirement requirement : missing) addRow(requirement, false);
        for (PermissionRequest.Requirement requirement : optional) addRow(requirement, false);
        for (PermissionRequest.Requirement requirement : granted) addRow(requirement, true);
        continueButton.setEnabled(missing.isEmpty());
        continueButton.setAlpha(missing.isEmpty() ? 1f : 0.5f);
    }

    private void addRow(PermissionRequest.Requirement requirement, boolean granted) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_permission, rows, false);
        ImageView icon = row.findViewById(R.id.permissionIcon);
        TextView title = row.findViewById(R.id.permissionTitle);
        TextView reason = row.findViewById(R.id.permissionReason);
        TextView optionalTag = row.findViewById(R.id.permissionOptional);
        MaterialButton enable = row.findViewById(R.id.permissionEnable);
        ImageView check = row.findViewById(R.id.permissionGranted);

        icon.setImageResource(requirement.permission.icon);
        title.setText(requirement.permission.title);
        reason.setText(requirement.permission.reason);
        reason.setVisibility(granted ? View.GONE : View.VISIBLE);
        optionalTag.setVisibility(!granted && !requirement.required ? View.VISIBLE : View.GONE);
        enable.setVisibility(granted ? View.GONE : View.VISIBLE);
        check.setVisibility(granted ? View.VISIBLE : View.GONE);
        row.setAlpha(granted ? 0.6f : 1f);
        enable.setOnClickListener(v -> requirement.permission.request(this));
        rows.addView(row);
    }

    @Override
    public Activity activity() {
        return requireActivity();
    }

    @Override
    public void requestRuntimePermission(String permission) {
        runtimePermissionLauncher.launch(permission);
    }
}
