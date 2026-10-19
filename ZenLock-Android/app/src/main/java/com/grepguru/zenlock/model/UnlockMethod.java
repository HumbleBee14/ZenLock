package com.grepguru.zenlock.model;

import com.grepguru.zenlock.ui.AppLanguage;
import com.grepguru.zenlock.R;
/**
 * Enum representing different unlock methods available in ZenLock
 */
public enum UnlockMethod {
    PIN_UNLOCK(R.string.unlock_method_pin, R.string.unlock_method_pin_description, 0),
    ACCOUNTABILITY_PARTNER_OTP(R.string.unlock_method_partner, R.string.unlock_method_partner_description, 1);
    
    private final int displayName;
    private final int description;
    private final int securityLevel;
    
    UnlockMethod(int displayName, int description, int securityLevel) {
        this.displayName = displayName;
        this.description = description;
        this.securityLevel = securityLevel;
    }
    
    public String getDisplayName(android.content.Context context) {
        return AppLanguage.localizedContext(context).getString(displayName);
    }
    
    public String getDescription(android.content.Context context) {
        return AppLanguage.localizedContext(context).getString(description);
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
