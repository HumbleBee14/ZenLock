package com.grepguru.focuslock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.grepguru.focuslock.utils.ScheduleActivator;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed - resetting lock state");
            
            SharedPreferences preferences = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);

            // Reset Lock State after Reboot
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("wasDeviceRestarted", true); // Mark Restart
            editor.putBoolean("isLocked", false);
            editor.remove("lockEndTime");
            editor.apply();
            
            Log.d(TAG, "Lock state reset after boot");
            
            // Reactivate all schedules after boot
            try {
                ScheduleActivator activator = new ScheduleActivator(context);
                activator.rescheduleAllSchedules();
                Log.d(TAG, "Schedules reactivated after boot");
            } catch (Exception e) {
                Log.e(TAG, "Failed to reactivate schedules after boot", e);
            }
        }
    }
}
