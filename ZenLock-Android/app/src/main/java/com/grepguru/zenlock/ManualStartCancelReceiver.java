package com.grepguru.zenlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.grepguru.zenlock.utils.ManualStartDelayScheduler;

public class ManualStartCancelReceiver extends BroadcastReceiver {

    private static final String TAG = "ManualStartCancel";
    public static final String ACTION_CANCEL = "com.grepguru.zenlock.ACTION_CANCEL_MANUAL_START";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_CANCEL.equals(intent.getAction())) {
            return;
        }
        Log.d(TAG, "Cancelling pending manual start from notification");
        ManualStartDelayScheduler.cancelPendingSession(context);
    }
}
