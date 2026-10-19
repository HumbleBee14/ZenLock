package com.grepguru.zenlock.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import com.grepguru.zenlock.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "pt-rBR-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LocalizedLayoutTest {
    private View inflate(int layout) {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_ZenLock);
        View root = LayoutInflater.from(context).inflate(layout, new android.widget.FrameLayout(context), false);
        int width = Math.round(360 * context.getResources().getDisplayMetrics().density);
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        root.layout(0, 0, width, root.getMeasuredHeight());
        return root;
    }

    @Test public void durationControlsStayInsideCardOnNarrowScreen() {
        View root = inflate(R.layout.fragment_home);
        ViewGroup controls = root.findViewById(R.id.durationControlsContainer);
        for (int i = 0; i < controls.getChildCount(); i++) {
            View child = controls.getChildAt(i);
            assertTrue("Control extends past left edge: left=" + child.getLeft() + ", width=" + child.getWidth() + ", row=" + controls.getWidth(), child.getLeft() >= 0);
            assertTrue("Control extends past right edge", child.getRight() <= controls.getWidth());
        }
    }

    @Test public void wrappedTodayMetricLabelsRemainCentered() {
        View root = inflate(R.layout.fragment_analytics);
        for (int id : new int[]{R.id.todayTime, R.id.todayMobileUsage, R.id.todaySessions}) {
            ViewGroup column = (ViewGroup) root.findViewById(id).getParent();
            TextView label = (TextView) column.getChildAt(1);
            assertEquals("Wrapped metric label must stay centered", Layout.Alignment.ALIGN_CENTER,
                    label.getLayout().getParagraphAlignment(0));
        }
    }
}
