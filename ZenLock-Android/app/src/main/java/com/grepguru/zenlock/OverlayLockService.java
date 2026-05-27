package com.grepguru.zenlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.view.WindowManager;
import androidx.annotation.Nullable;

public class OverlayLockService extends Service {
    private static final String CHANNEL_ID = "zenlock_overlay_lock";
    private LockOverlayView overlayView;
    private WindowManager windowManager;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = new LockOverlayView(this);
        windowManager.addView(overlayView, overlayView.getLayoutParams());
        startForeground(1, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service is sticky to survive process death
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "ZenLock Overlay Lock",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ZenLock is active")
            .setContentText("Your device is locked.")
            .setSmallIcon(R.drawable.ic_lock);
        return builder.build();
    }

    // Static helper to (re)show the overlay from anywhere
    public static void showOverlay(Context context) {
        Intent intent = new Intent(context, OverlayLockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
