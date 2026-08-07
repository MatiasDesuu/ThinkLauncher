package org.matiasdesu.thinklauncherv2.utils;

public final class HomePositionHelper {

    private static final int[][] CELL_TO_POSITION = {
            { 0, 6, 1 },
            { 4, 8, 5 },
            { 2, 7, 3 }
    };

    private static final int[] POSITION_TO_HORIZONTAL = { 0, 2, 0, 2, 0, 2, 1, 1, 1 };
    private static final int[] POSITION_TO_VERTICAL = { 0, 0, 2, 2, 1, 1, 0, 2, 1 };

    public static final int DEFAULT_POSITION = 8;

    private HomePositionHelper() {
    }

    public static int sanitize(int position) {
        if (position < 0 || position > 8) {
            return DEFAULT_POSITION;
        }
        return position;
    }

    public static int positionFromAlignment(int vertical, int horizontal) {
        if (vertical < 0 || vertical > 2) {
            vertical = 1;
        }
        if (horizontal < 0 || horizontal > 2) {
            horizontal = 1;
        }
        return CELL_TO_POSITION[vertical][horizontal];
    }

    public static int horizontalFromPosition(int position) {
        return POSITION_TO_HORIZONTAL[sanitize(position)];
    }

    public static int verticalFromPosition(int position) {
        return POSITION_TO_VERTICAL[sanitize(position)];
    }
}
