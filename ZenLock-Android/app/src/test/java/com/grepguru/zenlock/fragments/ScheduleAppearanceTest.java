package com.grepguru.zenlock.fragments;

import static org.junit.Assert.*;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.grepguru.zenlock.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public class ScheduleAppearanceTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle state) {
            setTheme(R.style.Theme_ZenLock); super.onCreate(state);
            setContentView(new android.widget.FrameLayout(this));
            if(state==null)getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content,new ScheduleFragment(),"schedule").commitNow();
        }
    }
    @Test public void emptyScheduleScreenOffersReadableTemplatesWithoutEmptyOverview() {
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        View root=host.get().getSupportFragmentManager().findFragmentByTag("schedule").requireView();
        assertNotNull(root.findViewById(R.id.createScheduleBtn));
        assertEquals(View.GONE,root.findViewById(R.id.upNextGroup).getVisibility());
        android.widget.LinearLayout templates=root.findViewById(R.id.templateList);
        assertEquals(3,templates.getChildCount());
        assertNotNull(templates.getChildAt(0).findViewById(R.id.templateSummary));
        host.pause().stop().destroy();
    }
}
