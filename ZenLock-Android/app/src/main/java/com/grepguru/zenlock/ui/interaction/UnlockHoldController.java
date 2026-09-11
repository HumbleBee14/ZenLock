package com.grepguru.zenlock.ui.interaction;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;
import com.grepguru.zenlock.R;

/** Owns the unlock button's gesture, progress, and cancellable delayed callback. */
public final class UnlockHoldController {
    private final Button button;
    private final Runnable onStart;
    private final Runnable onReady;
    private final Runnable onCancelled;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HoldGate gate = new HoldGate();
    private final Runnable tick = this::update;
    private boolean ready;
    private final UnlockHoldOverlay overlay;

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    public UnlockHoldController(Button button, Runnable onStart, Runnable onReady, Runnable onCancelled) {
        this.button = button;
        this.onStart = onStart;
        this.onReady = onReady;
        this.onCancelled = onCancelled;
        overlay = new UnlockHoldOverlay(button);
        button.setOnClickListener(v -> {
            if (ready) { ready = false; onReady.run(); }
        });
        // Assistive activation offers the same five-second delay with an explicit cancel action.
        androidx.core.view.ViewCompat.replaceAccessibilityAction(button,
                androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                button.getContext().getString(R.string.unlock_accessible_wait), (view, arguments) -> {
                    if (gate.isHolding()) cancelInteraction(); else start();
                    return true;
                });
        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: start(); return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getX() < 0 || event.getY() < 0 || event.getX() > v.getWidth() || event.getY() > v.getHeight()) cancelInteraction();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_DOWN: cancelInteraction(); return true;
                default: return true;
            }
        });
        button.setOnKeyListener((v, key, event) -> {
            if (key != KeyEvent.KEYCODE_ENTER && key != KeyEvent.KEYCODE_DPAD_CENTER && key != KeyEvent.KEYCODE_SPACE) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) start();
            else if (event.getAction() == KeyEvent.ACTION_UP) cancelInteraction();
            return true;
        });
    }

    private void start() {
        if (gate.isHolding()) return;
        onStart.run();
        gate.start(SystemClock.elapsedRealtime());
        button.setPressed(true);
        overlay.show();
        update();
    }

    private void update() {
        if (!gate.isHolding()) return;
        if (!button.isShown() || !button.hasWindowFocus()) { cancel(); return; }
        long now = SystemClock.elapsedRealtime();
        if (gate.complete(now)) {
            button.setPressed(false);
            overlay.dismiss();
            ready = true;
            button.performClick();
            return;
        }
        overlay.update(gate.remaining(now));
        handler.postDelayed(tick, 50);
    }

    private void cancelInteraction() {
        boolean wasHolding = gate.isHolding();
        cancel();
        if (wasHolding) onCancelled.run();
    }

    public void cancel() {
        handler.removeCallbacks(tick);
        gate.cancel();
        ready = false;
        button.setPressed(false);
        overlay.dismiss();
    }
}
