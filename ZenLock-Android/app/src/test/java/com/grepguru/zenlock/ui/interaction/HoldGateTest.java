package com.grepguru.zenlock.ui.interaction;
import org.junit.Test;
import static org.junit.Assert.*;

public class HoldGateTest {
    @Test public void requiresFiveFullSecondsAndOnlyCompletesOnce() {
        HoldGate gate = new HoldGate();
        gate.start(100);
        assertFalse(gate.complete(5099));
        assertTrue(gate.complete(5100));
        assertFalse(gate.complete(5101));
    }
    @Test public void releaseCancelsAndNextHoldStartsFromZero() {
        HoldGate gate = new HoldGate();
        gate.start(0);
        gate.cancel();
        assertFalse(gate.complete(6000));
        gate.start(6000);
        assertFalse(gate.complete(10000));
        assertTrue(gate.complete(11000));
    }
    @Test public void repeatedKeyDownDoesNotResetHold() {
        HoldGate gate = new HoldGate();
        gate.start(0);
        gate.start(4000);
        assertTrue(gate.complete(5000));
    }
}
