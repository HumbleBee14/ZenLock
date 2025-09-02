package com.grepguru.zenlock.model;

import android.graphics.drawable.Drawable;

public class SelectableAppModel {
    private String packageName;
    private String appName;
    private boolean isDefault;
    private boolean isSelected;
    private Drawable icon; // App icon

    public SelectableAppModel(String packageName, String appName, boolean isDefault, boolean isSelected, Drawable icon) {
        this.packageName = packageName;
        this.appName = appName;
        this.isDefault = isDefault;
        this.isSelected = isSelected;
        this.icon = icon;
    }

    public String getPackageName() { return packageName; }

    public String getAppName() { return appName; }

    public boolean isDefault() { return isDefault; }

    public boolean isSelected() { return isSelected; }

    public void setSelected(boolean selected) { this.isSelected = selected; }

    public Drawable getIcon() { return icon; }
}
