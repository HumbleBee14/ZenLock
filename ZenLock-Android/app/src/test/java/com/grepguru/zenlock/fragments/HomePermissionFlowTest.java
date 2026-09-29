package com.grepguru.zenlock.fragments;

import static org.junit.Assert.*;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.grepguru.zenlock.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class HomePermissionFlowTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle saved) {
            setTheme(R.style.Theme_ZenLock);
            super.onCreate(saved);
            setContentView(new android.widget.FrameLayout(this));
            if (saved == null) getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content,new HomeFragment(),"home").commitNow();
        }
    }
    @Test public void interruptedSetupRetainsSheetAndChosenDuration() {
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        HomeFragment home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        ReflectionHelpers.callInstanceMethod(home,"setTimeInMinutes",ReflectionHelpers.ClassParameter.from(int.class,35));
        ReflectionHelpers.callInstanceMethod(home,"checkAndStartLockService");
        host.get().getSupportFragmentManager().executePendingTransactions();
        host.recreate();
        host.get().getSupportFragmentManager().executePendingTransactions();
        home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        assertEquals(35,(int)ReflectionHelpers.getField(home,"selectedMinutes"));
        assertNotNull(host.get().getSupportFragmentManager().findFragmentByTag("PermissionSheet"));
        host.pause().stop().destroy();
    }
    @Test public void completingHoldDoesNotClaimSessionAlreadyStarted() {
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        HomeFragment home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        ReflectionHelpers.callInstanceMethod(home,"completeZenActivation");
        String text=((TextView)home.requireView().findViewById(R.id.zenProgressMessage)).getText().toString();
        assertFalse(text.contains("Activated") || text.contains("scheduled"));
        assertFalse(host.get().getSharedPreferences("FocusLockPrefs",0).getBoolean("isLocked",false));
        host.pause().stop().destroy();
    }
    @Test public void timerCardKeepsItsHeightAcrossModes() {
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        HomeFragment home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        android.view.View root=home.requireView();
        int width=(int)(360*host.get().getResources().getDisplayMetrics().density);
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
        int before=root.findViewById(R.id.timerCard).getMeasuredHeight();
        root.findViewById(R.id.modeToggleButton).performClick();
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
        assertEquals(before,root.findViewById(R.id.timerCard).getMeasuredHeight());
        host.pause().stop().destroy();
    }

    @Test public void inlineClockKeepsMidnightNoonAndMinutesInSyncAfterRecreation() {
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        HomeFragment home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        home.requireView().findViewById(R.id.modeToggleButton).performClick();
        ReflectionHelpers.setField(home,"lockUntilHour",0);
        ReflectionHelpers.setField(home,"lockUntilMinute",58);
        ReflectionHelpers.callInstanceMethod(home,"updateLockUntilDisplay");
        android.widget.NumberPicker hours=home.requireView().findViewById(R.id.lockUntilHoursPicker);
        android.widget.NumberPicker minutes=home.requireView().findViewById(R.id.lockUntilMinutesPicker);
        assertEquals(12,hours.getValue());
        assertEquals(58,minutes.getValue());
        home.requireView().findViewById(R.id.lockUntilPeriodButton).performClick();
        assertEquals(12,(int)ReflectionHelpers.getField(home,"lockUntilHour"));
        minutes.setValue(59);
        android.widget.NumberPicker.OnValueChangeListener change=ReflectionHelpers.getField(minutes,"mOnValueChangeListener");
        change.onValueChange(minutes,58,59);
        assertEquals(59,(int)ReflectionHelpers.getField(home,"lockUntilMinute"));
        host.recreate();
        home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        hours=home.requireView().findViewById(R.id.lockUntilHoursPicker);
        minutes=home.requireView().findViewById(R.id.lockUntilMinutesPicker);
        assertEquals(12,hours.getValue()); assertEquals(59,minutes.getValue());
        assertEquals("PM",((android.widget.TextView)home.requireView().findViewById(R.id.lockUntilPeriodButton)).getText().toString());
        hours.performClick();
        home.getChildFragmentManager().executePendingTransactions();
        com.google.android.material.timepicker.MaterialTimePicker popup=(com.google.android.material.timepicker.MaterialTimePicker)home.getChildFragmentManager().findFragmentByTag("lockUntilPicker");
        assertNotNull(popup); assertEquals(12,popup.getHour()); assertEquals(59,popup.getMinute());
        host.pause().stop().destroy();
    }

    @Test public void durationPresetsExcludeTheTestingMinute() {
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        HomeFragment home=(HomeFragment)host.get().getSupportFragmentManager().findFragmentByTag("home");
        android.widget.NumberPicker minutes=home.requireView().findViewById(R.id.minutesPicker);
        assertArrayEquals(new String[]{"0","5","10","15","20","30","40","50"},minutes.getDisplayedValues());
        host.pause().stop().destroy();
    }

}
