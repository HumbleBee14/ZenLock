package com.grepguru.zenlock.permissions;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public final class PermissionGate {
    private PermissionGate() {}

    public static void ensure(Fragment owner, PermissionRequest request, String resultKey, Bundle payload) {
        if (request.requiredGranted(owner.requireContext())) {
            owner.getParentFragmentManager().setFragmentResult(resultKey, payload);
            return;
        }
        PermissionSheet.show(owner.requireActivity(), request, resultKey, payload);
    }

    public static void review(FragmentActivity activity) {
        PermissionSheet.show(activity, FeaturePermissions.all(), null, new Bundle());
    }
}
