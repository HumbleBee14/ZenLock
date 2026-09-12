package com.grepguru.zenlock.permissions;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public final class PermissionGate {

    private PermissionGate() {}

    public static void ensure(FragmentActivity activity, PermissionRequest request, @Nullable Runnable onReady) {
        if (request.requiredGranted(activity)) {
            if (onReady != null) onReady.run();
            return;
        }
        PermissionSheet.show(activity, request, onReady);
    }

    public static void review(FragmentActivity activity) {
        PermissionSheet.show(activity, FeaturePermissions.all(), null);
    }
}
