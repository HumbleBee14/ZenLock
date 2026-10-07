package com.grepguru.zenlock.admin;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.grepguru.zenlock.R;

public final class UninstallProtectionReceiver extends DeviceAdminReceiver {
    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {
        return context.getString(R.string.uninstall_protection_disable_warning);
    }
}
