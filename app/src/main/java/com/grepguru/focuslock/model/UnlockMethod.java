package com.grepguru.focuslock.model;

/**
 * Enum representing different unlock methods available in Focus Lock
 */
public enum UnlockMethod {
    PIN_UNLOCK("PIN Unlock", "Use your set PIN to unlock", 0),
    ACCOUNTABILITY_PARTNER_OTP("Accountability Partner", "Get temporary code from trusted contact", 1);
    
    private final String displayName;
    private final String description;
    private final int securityLevel;
    
    UnlockMethod(String displayName, String description, int securityLevel) {
        this.displayName = displayName;
        this.description = description;
        this.securityLevel = securityLevel;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getSecurityLevel() {
        return securityLevel;
    }
    
    public static UnlockMethod fromSecurityLevel(int level) {
        for (UnlockMethod method : values()) {
            if (method.securityLevel == level) {
                return method;
            }
        }
        return PIN_UNLOCK; // Default
    }
}
