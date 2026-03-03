package com.grepguru.zenlock.utils;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Utility class for detecting MIUI/HyperOS specific restrictions.
 *
 * MIUI has a proprietary permission "Display pop-up windows while running in the background"
 * (internal opcode 10021) which is OFF by default. When disabled, startActivity() calls
 * from background services (including AccessibilityService) are SILENTLY dropped —
 * no exception, no log, the intent just vanishes.
 *
 * This affects Xiaomi, Redmi, and POCO devices running MIUI or HyperOS.
 */
public class MiuiUtils {

    private static final String TAG = "MiuiUtils";
    private static final int OP_BACKGROUND_START_ACTIVITY = 10021;

    /**
     * Check if the device is a Xiaomi/Redmi/POCO device
     */
    public static boolean isXiaomiDevice() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco");
    }

    /**
     * Check if the "Display pop-up windows while running in background" permission is granted.
     * This is a MIUI-specific permission (opcode 10021) that controls whether an app
     * can launch activities from background services.
     *
     * @return true if permission is granted or device is not Xiaomi, false if denied on Xiaomi
     */
    public static boolean canStartActivityFromBackground(Context context) {
        if (!isXiaomiDevice()) {
            return true;
        }
        try {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager == null) return true;

            Method checkOpMethod = AppOpsManager.class.getDeclaredMethod(
                "checkOpNoThrow", int.class, int.class, String.class);
            int result = (int) checkOpMethod.invoke(appOpsManager,
                OP_BACKGROUND_START_ACTIVITY,
                android.os.Process.myUid(),
                context.getPackageName());
            boolean allowed = result == AppOpsManager.MODE_ALLOWED;
            Log.d(TAG, "MIUI background start activity permission: " + (allowed ? "ALLOWED" : "DENIED"));
            return allowed;
        } catch (Exception e) {
            // Reflection failed — could be non-MIUI ROM on Xiaomi hardware, assume allowed
            Log.d(TAG, "Could not check MIUI permission (may not be MIUI ROM): " + e.getMessage());
            return true;
        }
    }

    /**
     * Open MIUI's permission editor for this app where user can enable
     * "Display pop-up windows while running in background"
     */
    public static void openMiuiPermissionEditor(Context context) {
        // Try MIUI permission editor (newer MIUI versions)
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        } catch (Exception e) {
            Log.d(TAG, "PermissionsEditorActivity not found, trying alternative");
        }

        // Try alternative MIUI permission editor
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        } catch (Exception e) {
            Log.d(TAG, "AppPermissionsEditorActivity not found, trying app info");
        }

        // Final fallback: open app info settings
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open any settings page", e);
        }
    }

    /**
     * Open MIUI Autostart settings
     */
    public static void openAutoStartSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to app settings
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
