package com.grepguru.zenlock;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33, 36})
public class LockScreenActivityTest {
    @Before
    public void prepareKeyguard() {
        // Robolectric's unlocked-keyguard shadow does not support a null dismiss callback.
        shadowOf(RuntimeEnvironment.getApplication().getSystemService(KeyguardManager.class))
                .setKeyguardLocked(true);
    }

    @After
    public void resetActiveScreen() {
        ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", false);
    }

    @Test
    public void rejectedDuplicateToleratesLateCallbacks() {
        ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", true);
        ActivityController<LockScreenActivity> controller =
                Robolectric.buildActivity(LockScreenActivity.class).create();
        LockScreenActivity activity = controller.get();
        assertTrue(activity.isFinishing());
        shadowOf(activity.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
        // Explicitly inject the unexpected resume seen in the production report.
        activity.onResume();
        activity.onPause();
        activity.onStop();
        controller.destroy();
        assertTrue("A rejected duplicate must not clear the live owner's state",
                LockScreenActivity.isActive());
    }

    @Test
    public void rejectedDuplicateStillInitializesPreferences() {
        ReflectionHelpers.setStaticField(LockScreenActivity.class, "isLockScreenActive", true);
        ActivityController<LockScreenActivity> controller =
                Robolectric.buildActivity(LockScreenActivity.class).create();
        assertNotNull(ReflectionHelpers.getField(controller.get(), "preferences"));
        controller.destroy();
        assertTrue(LockScreenActivity.isActive());
    }

    @Test
    public void activeLockSurvivesBackAndDuplicateDestruction() {
        SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("isLocked", true)
                .putLong("lockEndTime", System.currentTimeMillis() + 900_000)
                .putLong("lockTargetDuration", 900_000).commit();
        ActivityController<LockScreenActivity> owner =
                Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        assertFalse(owner.get().isFinishing());
        owner.get().getOnBackPressedDispatcher().onBackPressed();
        assertFalse(owner.get().isFinishing());
        assertTrue(prefs.getBoolean("isLocked", false));
        assertNotNull(shadowOf(owner.get().getSystemService(NotificationManager.class))
                .getNotification(1001));

        ActivityController<LockScreenActivity> duplicate =
                Robolectric.buildActivity(LockScreenActivity.class).create();
        duplicate.destroy();
        assertTrue(LockScreenActivity.isActive());
        assertTrue(prefs.getBoolean("isLocked", false));
        owner.pause().stop().destroy();
        assertFalse(LockScreenActivity.isActive());
        assertTrue("Destroying the UI must not unlock the session", prefs.getBoolean("isLocked", false));
    }

    @Test public void pausingForAnotherWindowDoesNotRelaunchLockScreen() {
        SharedPreferences prefs = RuntimeEnvironment.getApplication().getSharedPreferences("FocusLockPrefs",0);
        prefs.edit().putBoolean("isLocked",true)
            .putLong("lockEndTime",System.currentTimeMillis()+900000)
            .putLong("lockTargetDuration",900000).commit();
        ActivityController<LockScreenActivity> owner=Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        LockScreenActivity a=owner.get();
        shadowOf(a.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
        shadowOf(a.getSystemService(android.os.PowerManager.class)).setIsInteractive(true);
        shadowOf(a.getSystemService(android.app.ActivityManager.class)).setProcesses(java.util.Collections.emptyList());
        while(shadowOf(a).getNextStartedActivity()!=null) {}
        owner.pause().stop();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));
        assertNull("The blocker owns enforcement; lifecycle transitions must not fight allowed apps",shadowOf(a).getNextStartedActivity());
        assertTrue(prefs.getBoolean("isLocked",false));
        owner.destroy();
    }

    @Test public void touchingScreenOutsideTimerRestoresHiddenControls() {
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("FocusLockPrefs",0);
        prefs.edit().putBoolean("isLocked",true).putLong("lockEndTime",System.currentTimeMillis()+900000)
            .putLong("lockTargetDuration",900000).commit();
        ActivityController<LockScreenActivity> owner=Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        LockScreenActivity a=owner.get();
        android.view.View unlock=a.findViewById(R.id.unlockArrow);
        android.view.View apps=a.findViewById(R.id.expandButtonContainer);
        android.view.View ends=a.findViewById(R.id.endsAtText);
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"hideChrome");
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
        assertEquals("End-time space stays reserved when hidden",android.view.View.INVISIBLE,ends.getVisibility());
        unlock.setVisibility(android.view.View.INVISIBLE);
        apps.setVisibility(android.view.View.INVISIBLE);
        android.view.MotionEvent tap=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,1,1,0);
        a.dispatchTouchEvent(tap);
        tap.recycle();
        assertEquals(android.view.View.VISIBLE,ends.getVisibility());
        assertEquals(android.view.View.VISIBLE,unlock.getVisibility());
        assertEquals(android.view.View.VISIBLE,apps.getVisibility());
        owner.pause().stop().destroy();
    }

    @Test public void visiblePinScreenIsNotReplacedButPausedScreenCanBeReopened() {
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("FocusLockPrefs",0);
        prefs.edit().putBoolean("isLocked",true).putLong("lockEndTime",System.currentTimeMillis()+900000)
            .putLong("lockTargetDuration",900000).commit();
        ActivityController<LockScreenActivity> owner=Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        LockScreenActivity a=owner.get();
        android.widget.EditText pin=a.findViewById(R.id.pinInput);
        pin.setText("12");
        AppBlockerServiceTest.RecordingBlocker service=Robolectric.buildService(AppBlockerServiceTest.RecordingBlocker.class).create().get();
        shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
        android.view.accessibility.AccessibilityEvent e=android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        e.setPackageName("example.blocked");e.setClassName("example.blocked.MainActivity");
        service.onAccessibilityEvent(e);
        assertEquals(0,service.launches.size());
        assertEquals("12",pin.getText().toString());
        owner.pause().stop();
        // The window event can precede onPause; it must be reconsidered after
        // the lifecycle settles even without a second accessibility event.
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));
        assertEquals(1,service.launches.size());
        assertEquals(0,service.launches.get(0).getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK);
        service.onDestroy();owner.destroy();e.recycle();
    }

    @Test public void appScrollArrowsTrackOverflowAndScrollPosition() {
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("FocusLockPrefs",0);
        prefs.edit().putBoolean("isLocked",true).putLong("lockEndTime",System.currentTimeMillis()+900000)
            .putLong("lockTargetDuration",900000).commit();
        ActivityController<LockScreenActivity> owner=Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        LockScreenActivity a=owner.get();
        androidx.recyclerview.widget.RecyclerView apps=a.findViewById(R.id.defaultAppsRecycler);
        java.util.List<com.grepguru.zenlock.model.AppModel> models=new java.util.ArrayList<>();
        for(int i=0;i<8;i++) models.add(new com.grepguru.zenlock.model.AppModel("example.app"+i,"App "+i,false,new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)));
        apps.setAdapter(new com.grepguru.zenlock.ui.adapter.AllowedAppsAdapter(a,models));
        android.view.View row=a.findViewById(R.id.allowedAppsRow);
        row.setVisibility(android.view.View.VISIBLE);
        int width=(int)(360*a.getResources().getDisplayMetrics().density);
        row.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
        row.layout(0,0,width,row.getMeasuredHeight());
        assertEquals("App viewport uses the entire available row",width,apps.getMeasuredWidth());
        assertEquals(android.view.View.INVISIBLE,a.findViewById(R.id.scrollAllowedAppsLeft).getVisibility());
        assertEquals(android.view.View.VISIBLE,a.findViewById(R.id.scrollAllowedAppsRight).getVisibility());
        apps.scrollBy(100000,0);
        assertEquals(android.view.View.VISIBLE,a.findViewById(R.id.scrollAllowedAppsLeft).getVisibility());
        assertEquals(android.view.View.INVISIBLE,a.findViewById(R.id.scrollAllowedAppsRight).getVisibility());
        models.subList(1,models.size()).clear();
        apps.getAdapter().notifyDataSetChanged();
        row.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
        row.layout(0,0,width,row.getMeasuredHeight());
        assertEquals(android.view.View.INVISIBLE,a.findViewById(R.id.scrollAllowedAppsLeft).getVisibility());
        assertEquals(android.view.View.INVISIBLE,a.findViewById(R.id.scrollAllowedAppsRight).getVisibility());
        owner.pause().stop().destroy();
    }

    @Test public void firstPinEyeTapRevealsDigitsWithoutChangingNumericKeyboard() {
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("FocusLockPrefs",0);
        prefs.edit().putBoolean("isLocked",true).putLong("lockEndTime",System.currentTimeMillis()+900000)
            .putLong("lockTargetDuration",900000).commit();
        ActivityController<LockScreenActivity> owner=Robolectric.buildActivity(LockScreenActivity.class).create().start().resume();
        android.widget.EditText pin=owner.get().findViewById(R.id.pinInput);
        pin.setText("1234"); pin.setSelection(2);
        owner.get().findViewById(R.id.pinVisibilityToggle).performClick();
        assertFalse(pin.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
        assertEquals(android.text.InputType.TYPE_CLASS_NUMBER,pin.getInputType() & android.text.InputType.TYPE_MASK_CLASS);
        assertEquals(2,pin.getSelectionStart());
        owner.get().findViewById(R.id.pinVisibilityToggle).performClick();
        assertTrue(pin.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
        assertEquals("1234",pin.getText().toString());
        assertEquals(2,pin.getSelectionStart());
        owner.pause().stop().destroy();
    }
    @Test public void expandedUnlockReturnsToArrowAfterInactivity() {
        ActivityController<LockScreenActivity> owner = openUnlockControls();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(5400));
        assertEquals(android.view.View.GONE, owner.get().findViewById(R.id.unlockExtendButtonContainer).getVisibility());
        assertEquals(android.view.View.VISIBLE, owner.get().findViewById(R.id.unlockArrow).getVisibility());
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
        assertEquals(android.view.View.INVISIBLE, owner.get().findViewById(R.id.unlockArrow).getVisibility());
        owner.pause().stop().destroy();
    }

    @Test public void interruptedUnlockHoldDoesNotLeaveControlsStuckOpen() {
        ActivityController<LockScreenActivity> owner = openUnlockControls();
        android.view.View button = owner.get().findViewById(R.id.unlockPromptButton);
        android.view.MotionEvent down = android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,10,10,0);
        button.dispatchTouchEvent(down); down.recycle();
        owner.windowFocusChanged(false);
        owner.windowFocusChanged(true);
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(5400));
        assertEquals(android.view.View.GONE, owner.get().findViewById(R.id.unlockExtendButtonContainer).getVisibility());
        owner.pause().stop().destroy();
    }

    @Test public void touchingExpandedControlsRestartsInactivityTimeout() {
        ActivityController<LockScreenActivity> owner = openUnlockControls();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3));
        android.view.MotionEvent tap = android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,1,1,0);
        owner.get().dispatchTouchEvent(tap); tap.recycle();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3));
        assertEquals(android.view.View.VISIBLE,owner.get().findViewById(R.id.unlockExtendButtonContainer).getVisibility());
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(2400));
        assertEquals(android.view.View.GONE,owner.get().findViewById(R.id.unlockExtendButtonContainer).getVisibility());
        owner.pause().stop().destroy();
    }

    @Test public void closingUnlockDialogRestartsTimeoutWithoutInterruptingEntry() {
        ActivityController<LockScreenActivity> owner = openUnlockControls();
        android.view.MotionEvent down = android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,10,10,0);
        owner.get().findViewById(R.id.unlockPromptButton).dispatchTouchEvent(down); down.recycle();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1100));
        com.grepguru.zenlock.utils.EnhancedUnlockManager manager=ReflectionHelpers.getField(owner.get(),"unlockManager");
        android.app.Dialog dialog=ReflectionHelpers.getField(manager,"currentDialog");
        assertNotNull(dialog);
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(6));
        assertTrue(dialog.isShowing());
        assertEquals(android.view.View.VISIBLE,owner.get().findViewById(R.id.unlockExtendButtonContainer).getVisibility());
        dialog.findViewById(R.id.cancelButton).performClick();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(5400));
        assertEquals(android.view.View.GONE,owner.get().findViewById(R.id.unlockExtendButtonContainer).getVisibility());
        owner.pause().stop().destroy();
    }

    private ActivityController<LockScreenActivity> openUnlockControls() {
        RuntimeEnvironment.getApplication().getSharedPreferences("FocusLockPrefs",0).edit()
            .putBoolean("isLocked",true).putLong("lockEndTime",System.currentTimeMillis()+900000)
            .putLong("lockTargetDuration",900000).commit();
        ActivityController<LockScreenActivity> owner=Robolectric.buildActivity(LockScreenActivity.class)
            .create().start().resume().visible().windowFocusChanged(true);
        owner.get().findViewById(R.id.unlockArrow).performClick();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(450));
        return owner;
    }

}
