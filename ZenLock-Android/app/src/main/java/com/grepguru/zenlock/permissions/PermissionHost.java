package com.grepguru.zenlock.permissions;

import android.app.Activity;

public interface PermissionHost {
    Activity activity();

    void requestRuntimePermission(String permission);
}
