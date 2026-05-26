package com.grepguru.zenlock.model;

import android.graphics.drawable.Drawable;

public class AppModel {
    private String packageName;
    private String appName;
    private boolean isDefault;
    private Drawable icon;

    public AppModel(String packageName, String appName, boolean isDefault, Drawable icon) {
        this.packageName = packageName;
        this.appName = appName;
        this.isDefault = isDefault;
        this.icon = icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Drawable getIcon() {
        return icon;
    }
}
