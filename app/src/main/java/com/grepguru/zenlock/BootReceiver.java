package com.grepguru.zenlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.grepguru.zenlock.utils.ScheduleActivator;

/**
 * BootReceiver - Handles device restart events
 * Reschedules all active focus schedules after device reboot
 * Also handles device restart detection for active focus sessions
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed, rescheduling focus sessions");
            
            // Mark device as restarted for active sessions
            markDeviceRestarted(context);
            
            // Reschedule all enabled schedules
            rescheduleAllSchedules(context);

            // Enforce lock overlay and lock screen if session is active and user enabled auto-restart
            SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
            boolean isLocked = prefs.getBoolean("isLocked", false);
            boolean autoRestart = prefs.getBoolean("auto_restart", false);
            Log.d(TAG, "Auto-restart preference: " + autoRestart);
            Log.d(TAG, "Lock state: " + isLocked);
            if (isLocked && autoRestart) {
                // Start overlay lock service
                Intent overlayIntent = new Intent(context, OverlayLockService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(overlayIntent);
                } else {
                    context.startService(overlayIntent);
                }
                // Bring up lock screen activity
                Intent lockIntent = new Intent(context, LockScreenActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(lockIntent);
                Log.d(TAG, "Lock session active after boot: started overlay and lock screen");
            }
        }
    }
    
    /**
     * Mark device as restarted so active focus sessions can detect it
     */
    private void markDeviceRestarted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean isLocked = prefs.getBoolean("isLocked", false);
        
        if (isLocked) {
            Log.d(TAG, "Active focus session detected, marking device as restarted");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("wasDeviceRestarted", true);
            editor.apply();
        }
    }
    
    /**
     * Reschedule all enabled focus schedules
     */
    private void rescheduleAllSchedules(Context context) {
        try {
            ScheduleActivator activator = new ScheduleActivator(context);
            activator.rescheduleAllSchedules();
            Log.d(TAG, "Successfully rescheduled all focus schedules");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reschedule focus schedules after boot", e);
        }
    }
}