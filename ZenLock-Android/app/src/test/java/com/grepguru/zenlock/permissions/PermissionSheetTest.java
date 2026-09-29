package com.grepguru.zenlock.permissions;

import static org.junit.Assert.*;
import android.os.Bundle;
import android.os.Parcel;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.grepguru.zenlock.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PermissionSheetTest {
    public static class Host extends AppCompatActivity {
        int deliveries;
        Bundle result;
        @Override public void onCreate(Bundle state) {
            setTheme(R.style.Theme_ZenLock);
            super.onCreate(state);
            setContentView(new android.widget.FrameLayout(this));
            getSupportFragmentManager().setFragmentResultListener("test",this,(key,value)->{
                deliveries++;
                result=value;
            });
        }
    }
    @Test public void savedRequestRetainsRequiredGateOptionalLabelAndActionPayload() {
        ShadowSettings.setCanDrawOverlays(false);
        ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        Bundle payload=new Bundle(); payload.putInt("minutes",35);
        PermissionSheet.show(host.get(),PermissionRequest.titled("Focus setup")
            .require(AppPermission.OVERLAY).recommend(AppPermission.USAGE_ACCESS).build(),"test",payload);
        host.get().getSupportFragmentManager().executePendingTransactions();
        Bundle saved=new Bundle();
        host.pause().saveInstanceState(saved).stop().destroy();
        // Parcel saved state so restoration cannot depend on the original Java objects.
        Parcel parcel=Parcel.obtain(); parcel.writeBundle(saved); parcel.setDataPosition(0);
        Bundle restored=parcel.readBundle(Host.class.getClassLoader()); parcel.recycle();
        host=Robolectric.buildActivity(Host.class).create(restored).start().resume().visible();
        PermissionSheet sheet=(PermissionSheet)host.get().getSupportFragmentManager().findFragmentByTag("PermissionSheet");
        assertNotNull(sheet);
        View button=sheet.requireView().findViewById(R.id.permissionsContinue);
        assertFalse(button.isEnabled());
        LinearLayout rows=sheet.requireView().findViewById(R.id.permissionRows);
        assertEquals(View.GONE,rows.getChildAt(0).findViewById(R.id.permissionOptional).getVisibility());
        assertEquals(View.VISIBLE,rows.getChildAt(1).findViewById(R.id.permissionOptional).getVisibility());
        assertEquals(0,host.get().deliveries);
        ShadowSettings.setCanDrawOverlays(true);
        host.pause().resume();
        assertTrue(button.isEnabled());
        assertEquals("Granting a permission must not auto-start the action",0,host.get().deliveries);
        button.performClick();
        assertEquals(1,host.get().deliveries);
        assertEquals(35,host.get().result.getInt("minutes"));
        host.get().getSupportFragmentManager().executePendingTransactions();
        assertNull(host.get().getSupportFragmentManager().findFragmentByTag("PermissionSheet"));
        host.pause().stop().destroy();
    }
}
