package com.grepguru.zenlock.ui.interaction;
import org.junit.Test;
import static org.junit.Assert.*;

public class HoldGateTest {
    @Test public void requiresOneFullSecondAndOnlyCompletesOnce() {
        HoldGate gate = new HoldGate();
        gate.start(100);
        assertFalse(gate.complete(1099));
        assertTrue(gate.complete(1100));
        assertFalse(gate.complete(1101));
    }
    @Test public void releaseCancelsAndNextHoldStartsFromZero() {
        HoldGate gate = new HoldGate();
        gate.start(0);
        gate.cancel();
        assertFalse(gate.complete(6000));
        gate.start(6000);
        assertFalse(gate.complete(6999));
        assertTrue(gate.complete(7000));
    }
    @Test public void repeatedKeyDownDoesNotResetHold() {
        HoldGate gate = new HoldGate();
        gate.start(0);
        gate.start(500);
        assertTrue(gate.complete(1000));
    }
}
