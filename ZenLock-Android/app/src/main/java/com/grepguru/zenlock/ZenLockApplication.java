package com.grepguru.zenlock;

import android.app.Application;
import com.grepguru.zenlock.ui.ThemePreference;

public final class ZenLockApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ThemePreference.apply(this);
    }
}
