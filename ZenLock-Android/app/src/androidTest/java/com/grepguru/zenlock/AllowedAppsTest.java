package com.grepguru.zenlock;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;
import androidx.appcompat.widget.SwitchCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.grepguru.zenlock.model.SelectableAppModel;
import com.grepguru.zenlock.ui.adapter.WhitelistAdapter;
import com.grepguru.zenlock.utils.WhitelistManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AllowedAppsTest {
    @Test
    public void acceptsEightRejectsNinthAndAllowsReplacement() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_ZenLock);
            List<SelectableAppModel> apps = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                apps.add(new SelectableAppModel("test.app" + i, "App " + i, false, false, null));
            }
            Set<String> selected = new HashSet<>();
            WhitelistAdapter adapter = new WhitelistAdapter(apps, selected, 8);
            WhitelistAdapter.ViewHolder holder = adapter.onCreateViewHolder(new FrameLayout(context), 0);
            SwitchCompat toggle = holder.itemView.findViewById(R.id.appCheckBox);
            for (int i = 0; i < 8; i++) {
                adapter.onBindViewHolder(holder, i);
                toggle.setChecked(true);
            }
            assertEquals(8, selected.size());
            adapter.onBindViewHolder(holder, 8);
            toggle.setChecked(true);
            assertFalse(toggle.isChecked());
            assertFalse(selected.contains("test.app8"));
            assertEquals(8, selected.size());
            adapter.onBindViewHolder(holder, 0);
            assertTrue(toggle.isChecked());
            toggle.setChecked(false);
            adapter.onBindViewHolder(holder, 8);
            toggle.setChecked(true);
            assertEquals(8, selected.size());
            assertFalse(selected.contains("test.app0"));
            assertTrue(selected.contains("test.app8"));
        });
    }

    @Test
    public void runningVariantIsAlwaysAllowed() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(WhitelistManager.isAppWhitelisted(context, context.getPackageName()));
    }
}
