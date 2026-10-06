package com.grepguru.zenlock.admin;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33, 36})
public class UninstallProtectionTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Test public void defaultOffAndRequestRequiresSystemConsent() {
        assertFalse(UninstallProtection.isEnabled(context));
        Intent intent = UninstallProtection.activationIntent(context);
        assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, intent.getAction());
        assertEquals(UninstallProtection.component(context),
                intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN));
        assertFalse("Creating the request must not activate admin", UninstallProtection.isEnabled(context));
    }

    @Test public void disablingAdminPreservesFocusSession() {
        DevicePolicyManager manager = context.getSystemService(DevicePolicyManager.class);
        shadowOf(manager).setActiveAdmin(UninstallProtection.component(context));
        context.getSharedPreferences("FocusLockPrefs", 0).edit().putBoolean("isLocked", true).commit();
        assertTrue(UninstallProtection.isEnabled(context));
        UninstallProtection.disable(context);
        assertFalse(UninstallProtection.isEnabled(context));
        assertTrue(context.getSharedPreferences("FocusLockPrefs", 0).getBoolean("isLocked", false));
    }

    @Test public void trustedAdminWindowIsAllowedButOtherSettingsAndSpoofedAppsAreNot() {
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = "com.android.settings";
        info.activityInfo.name = "com.android.settings.DeviceAdminAdd";
        info.activityInfo.applicationInfo = new ApplicationInfo();
        info.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(
                UninstallProtection.activationIntent(context), info);
        assertTrue(UninstallProtection.isManagementWindow(context, info.activityInfo.packageName, info.activityInfo.name));
        assertFalse(UninstallProtection.isManagementWindow(context, "com.android.settings", "com.android.settings.Settings"));
        assertFalse(UninstallProtection.isManagementWindow(context, "example.fake", info.activityInfo.name));
        info.activityInfo.applicationInfo.flags = 0;
        assertFalse(UninstallProtection.isManagementWindow(context, info.activityInfo.packageName, info.activityInfo.name));
    }
    @Test public void sharedSettingsAliasTargetStaysBlockedAndSystemInstallerRemainsAccessible() {
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = "example.systemsettings";
        info.activityInfo.name = "example.systemsettings.AdminAlias";
        info.activityInfo.targetActivity = "example.systemsettings.SubSettings";
        info.activityInfo.applicationInfo = new ApplicationInfo();
        info.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(
                UninstallProtection.activationIntent(context), info);
        assertFalse("Shared Settings alias targets must not expose unrelated settings",
                UninstallProtection.isManagementWindow(context,
                    info.activityInfo.packageName, info.activityInfo.targetActivity));
        assertFalse(UninstallProtection.isManagementWindow(context,
                info.activityInfo.packageName, "example.systemsettings.OtherActivity"));
        ResolveInfo installer = new ResolveInfo();
        installer.activityInfo = new ActivityInfo();
        installer.activityInfo.packageName = "example.systeminstaller";
        installer.activityInfo.name = "example.systeminstaller.UninstallActivity";
        installer.activityInfo.applicationInfo = new ApplicationInfo();
        installer.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(
                new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + context.getPackageName())), installer);
        assertTrue(UninstallProtection.isManagementWindow(context,
                installer.activityInfo.packageName, "android.app.Dialog"));
        installer.activityInfo.applicationInfo.flags = 0;
        assertFalse(UninstallProtection.isManagementWindow(context,
                installer.activityInfo.packageName, "android.app.Dialog"));
    }
}
