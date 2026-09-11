package com.grepguru.zenlock.ui.layout;

/** Layout policy shared by every row of the lock-screen app tray. */
public final class AllowedAppsGrid {
    public static final int COLUMNS = 4;

    private AllowedAppsGrid() {}

    public static int rowCount(int appCount) {
        return (Math.max(0, appCount) + COLUMNS - 1) / COLUMNS;
    }

    public static int itemsInRow(int appCount, int row) {
        return Math.max(0, Math.min(COLUMNS, appCount - row * COLUMNS));
    }

    public static float sideWeight(int appCount, int row) {
        return (COLUMNS - itemsInRow(appCount, row)) / 2f;
    }
}
