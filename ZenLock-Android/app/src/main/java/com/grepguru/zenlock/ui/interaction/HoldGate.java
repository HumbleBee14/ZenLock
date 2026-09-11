package com.grepguru.zenlock.ui.interaction;

/** Monotonic, single-use gate for an uninterrupted hold. */
public final class HoldGate {
    public static final long DURATION_MS = 5_000;
    private long started = -1;
    public void start(long now) { if (started < 0) started = now; }
    public void cancel() { started = -1; }
    public boolean isHolding() { return started >= 0; }
    public long remaining(long now) { return started < 0 ? DURATION_MS : Math.max(0, DURATION_MS - (now - started)); }
    public boolean complete(long now) {
        if (started < 0 || remaining(now) > 0) return false;
        cancel();
        return true;
    }
}
