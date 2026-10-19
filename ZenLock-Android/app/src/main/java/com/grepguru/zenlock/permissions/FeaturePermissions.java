package com.grepguru.zenlock.permissions;

import com.grepguru.zenlock.R;
import android.content.Context;
import android.content.SharedPreferences;

public final class FeaturePermissions {

    private FeaturePermissions() {}

    public static PermissionRequest focusSession(Context context, boolean delayedStart) {
        PermissionRequest.Builder builder = PermissionRequest.titled(R.string.background_to_start_a_focus_session)
                .action(R.string.background_start_session)
                .require(AppPermission.ACCESSIBILITY)
                .require(AppPermission.OVERLAY);
        if (delayedStart) builder.require(AppPermission.EXACT_ALARM);
        if (blocksNotifications(context)) builder.recommend(AppPermission.NOTIFICATION_ACCESS);
        builder.recommend(AppPermission.XIAOMI_BACKGROUND_POPUP);
        return builder.build();
    }

    public static PermissionRequest schedule(Context context) {
        PermissionRequest.Builder builder = PermissionRequest.titled(R.string.background_to_run_scheduled_sessions)
                .action(R.string.background_create_schedule)
                .require(AppPermission.ACCESSIBILITY)
                .require(AppPermission.OVERLAY)
                .require(AppPermission.EXACT_ALARM)
                .require(AppPermission.XIAOMI_BACKGROUND_POPUP)
                .recommend(AppPermission.UNRESTRICTED_BATTERY);
        if (blocksNotifications(context)) builder.recommend(AppPermission.NOTIFICATION_ACCESS);
        return builder.build();
    }

    public static PermissionRequest notificationBlocking() {
        return PermissionRequest.titled(R.string.background_to_block_notifications)
                .action(R.string.background_turn_on)
                .require(AppPermission.NOTIFICATION_ACCESS)
                .build();
    }

    public static PermissionRequest all() {
        PermissionRequest.Builder builder = PermissionRequest.titled(R.string.background_permissions);
        for (AppPermission permission : AppPermission.values()) builder.recommend(permission);
        return builder.build();
    }

    private static boolean blocksNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("block_notifications", true);
    }
}
