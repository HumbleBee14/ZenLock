package com.grepguru.zenlock.ui.layout;

import org.junit.Test;
import static org.junit.Assert.*;

public class AllowedAppsGridTest {
    @Test
    public void allSupportedCountsFitCenteredRowsWithoutLosingApps() {
        // Eight selected apps plus three enabled essential apps.
        for (int count = 0; count <= 11; count++) {
            int total = 0;
            for (int row = 0; row < AllowedAppsGrid.rowCount(count); row++) {
                int cells = AllowedAppsGrid.itemsInRow(count, row);
                assertTrue(cells >= 1 && cells <= 4);
                float side = AllowedAppsGrid.sideWeight(count, row);
                assertEquals(4f, side + cells + side, 0f);
                total += cells;
            }
            assertEquals(count, total);
        }
    }

    @Test
    public void partialRowsHaveSymmetricSpacing() {
        assertEquals(2, AllowedAppsGrid.rowCount(8));
        assertEquals(3, AllowedAppsGrid.rowCount(11));
        assertEquals(1.5f, AllowedAppsGrid.sideWeight(5, 1), 0f);
        assertEquals(1f, AllowedAppsGrid.sideWeight(6, 1), 0f);
        assertEquals(0.5f, AllowedAppsGrid.sideWeight(7, 1), 0f);
        assertEquals(0f, AllowedAppsGrid.sideWeight(8, 1), 0f);
    }
}
