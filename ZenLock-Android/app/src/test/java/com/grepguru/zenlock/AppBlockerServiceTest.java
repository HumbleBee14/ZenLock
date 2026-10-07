package com.grepguru.zenlock;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.time.Duration;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

// Regression tests for foreground classification and lock presentation.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33, 36})
public class AppBlockerServiceTest {
 public static class RecordingBlocker extends AppBlockerService {
   List<Intent> launches = new ArrayList<>();
   @Override public void startActivity(Intent intent) { launches.add(intent); }
 }
 RecordingBlocker blocker;
 SharedPreferences prefs;
 @Before public void setUp() {
   blocker = Robolectric.buildService(RecordingBlocker.class).create().get();
   shadowOf(blocker.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
   shadowOf(blocker.getSystemService(InputMethodManager.class)).setEnabledInputMethodInfoList(Collections.emptyList());
   prefs = blocker.getSharedPreferences("FocusLockPrefs", 0);
   prefs.edit().clear().putBoolean("isLocked",true)
      .putStringSet("whitelisted_apps", Collections.singleton("example.allowed")).commit();
   ReflectionHelpers.setStaticField(LockScreenActivity.class,"isLockScreenActive",false);
 }
 @After public void cleanUp() {
   ReflectionHelpers.setStaticField(LockScreenActivity.class,"isLockScreenActive",false);
 }
 void event(String pkg,String cls) {
   sendEvent(pkg,cls);
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
 }
 void sendEvent(String pkg,String cls) {
   AccessibilityEvent e=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
   e.setPackageName(pkg); e.setClassName(cls); blocker.onAccessibilityEvent(e); e.recycle();
 }
 @Test public void allowedNormalActivityIsNotBlocked_control() {
   event("example.allowed","example.allowed.MainActivity");
   assertEquals(0,blocker.launches.size());
 }
 @Test public void allowedLauncherActivityIsNotMistakenForHome() {
   event("example.allowed","example.allowed.LauncherActivity");
   assertEquals(0,blocker.launches.size());
 }
 @Test public void blockedEventBurstRequestsOnlyOneNonDestructiveLaunch() {
   ReflectionHelpers.setStaticField(LockScreenActivity.class,"isLockScreenActive",true);
   event("example.blocked","example.blocked.MainActivity");
   event("example.blocked.other","example.blocked.other.MainActivity");
   assertEquals(1,blocker.launches.size());
   assertEquals(0,blocker.launches.get(0).getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK);
 }
 @Test public void oplusKeyboardIsAllowedWhenReportedAsEnabled_control() {
   InputMethodInfo ime=new InputMethodInfo("com.oplus.securitykeyboard","SecureIME","Secure keyboard",null);
   shadowOf(blocker.getSystemService(InputMethodManager.class)).setEnabledInputMethodInfoList(Collections.singletonList(ime));
   event("com.oplus.securitykeyboard","android.inputmethodservice.SoftInputWindow");
   assertEquals(0,blocker.launches.size());
 }
 @Test public void oplusKeyboardIsBlockedIfNotReportedAsEnabled_conditionalOnly() {
   event("com.oplus.securitykeyboard","android.inputmethodservice.SoftInputWindow");
   assertEquals(1,blocker.launches.size());
 }
 @Test public void allowedAppCancelsPendingFallback() {
   event("example.blocked","example.blocked.MainActivity");
   event("example.allowed","example.allowed.MainActivity");
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }

 @Test public void homeWithGenericClassRemainsBlocked() {
   event("com.android.launcher", "android.widget.FrameLayout");
   assertEquals(1, blocker.launches.size());
 }
 @Test public void allowHomePreferenceIsRespected() {
   prefs.edit().putBoolean("allow_launcher_during_lock",true).commit();
   event("com.android.launcher", "com.android.launcher.Launcher");
   assertEquals(0, blocker.launches.size());
 }
 @Test public void systemUiRecentsRemainsBlocked() {
   event("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
   assertEquals(1, blocker.launches.size());
 }
 @Test public void backgroundClickDoesNotChangeForegroundDecision() {
   AccessibilityEvent e=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED);
   e.setPackageName("example.blocked"); e.setClassName("android.widget.Button");
   blocker.onAccessibilityEvent(e);
   assertEquals(0,blocker.launches.size());
 }
 @Test public void stillBlockedGetsFallback() {
   event("example.blocked","example.blocked.MainActivity");
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNotNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }
 @Test public void destroyedServiceCancelsFallback() {
   event("example.blocked","example.blocked.MainActivity");
   blocker.onDestroy();
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }
 @Test public void keyguardCancelsFallback() {
   event("example.blocked","example.blocked.MainActivity");
   shadowOf(blocker.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }
 @Test public void unlockCancelsFallback() {
   event("example.blocked","example.blocked.MainActivity");
   prefs.edit().putBoolean("isLocked",false).commit();
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }

 @Test public void keyboardWindowDoesNotCancelUnderlyingBlockedApp() {
   event("example.blocked","example.blocked.MainActivity");
   InputMethodInfo ime=new InputMethodInfo("com.oplus.securitykeyboard","SecureIME","Secure keyboard",null);
   shadowOf(blocker.getSystemService(InputMethodManager.class)).setEnabledInputMethodInfoList(Collections.singletonList(ime));
   event("com.oplus.securitykeyboard","android.inputmethodservice.SoftInputWindow");
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNotNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }
 @Test public void systemUiWindowDoesNotCancelUnderlyingBlockedApp() {
   event("example.blocked","example.blocked.MainActivity");
   event("com.android.systemui","android.widget.FrameLayout");
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNotNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }
 @Test public void customHomeResolvedByAndroidRemainsBlocked() {
   android.content.pm.ResolveInfo info=new android.content.pm.ResolveInfo();
   info.activityInfo=new android.content.pm.ActivityInfo();
   info.activityInfo.packageName="example.customhome";
   info.activityInfo.name="example.customhome.HomeActivity";
   shadowOf(blocker.getPackageManager()).addResolveInfoForIntent(
       new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),info);
   prefs.edit().putStringSet("whitelisted_apps",Collections.singleton("example.customhome")).commit();
   event("example.customhome","example.customhome.HomeActivity");
   assertEquals(1,blocker.launches.size());
 }
 @Test public void screenOffCancelsFallback() {
   event("example.blocked","example.blocked.MainActivity");
   shadowOf(blocker.getSystemService(android.os.PowerManager.class)).setIsInteractive(false);
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1501));
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }

 @Test public void transientLauncherBeforeAllowedAppDoesNotFlashLock() {
   sendEvent("com.android.launcher","com.android.launcher.Launcher");
   sendEvent("example.allowed","example.allowed.MainActivity");
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000));
   assertEquals(0,blocker.launches.size());
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
 }

 @Test public void scheduledStartupAndWindowEventShareOneLaunch() {
   prefs.edit().putString("current_session_source","schedule:1")
       .putLong("lockEndTime",System.currentTimeMillis()+900000).commit();
   blocker.onServiceConnected();
   sendEvent("example.blocked","example.blocked.MainActivity");
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));
   assertEquals(1,blocker.launches.size());
   blocker.onDestroy();
 }
 @Test public void adminControlsCancelPendingBlockButOrdinarySettingsStayBlocked() {
   blocker.onServiceConnected();
   android.content.pm.ResolveInfo info = new android.content.pm.ResolveInfo();
   info.activityInfo = new android.content.pm.ActivityInfo();
   info.activityInfo.packageName = "com.android.settings";
   info.activityInfo.name = "com.android.settings.DeviceAdminAdd";
   info.activityInfo.applicationInfo = new android.content.pm.ApplicationInfo();
   info.activityInfo.applicationInfo.flags = android.content.pm.ApplicationInfo.FLAG_SYSTEM;
   shadowOf(blocker.getPackageManager()).addResolveInfoForIntent(
       com.grepguru.zenlock.admin.UninstallProtection.activationIntent(blocker), info);
   shadowOf(blocker.getSystemService(android.app.admin.DevicePolicyManager.class))
       .setActiveAdmin(com.grepguru.zenlock.admin.UninstallProtection.component(blocker));
   blocker.sendBroadcast(new Intent(Intent.ACTION_PACKAGE_CHANGED, android.net.Uri.parse("package:com.android.settings")));
   shadowOf(Looper.getMainLooper()).idle();
   sendEvent("example.blocked", "example.blocked.MainActivity");
   event(info.activityInfo.packageName, info.activityInfo.name);
   shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000));
   assertEquals(0, blocker.launches.size());
   assertNull(shadowOf(blocker.getSystemService(NotificationManager.class)).getNotification(9999));
   event("com.android.settings", "com.android.settings.Settings");
   assertEquals(1, blocker.launches.size());
 }
}
