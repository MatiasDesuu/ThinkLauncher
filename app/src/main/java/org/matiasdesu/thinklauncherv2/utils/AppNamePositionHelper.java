package org.matiasdesu.thinklauncherv2.utils;

public class AppNamePositionHelper {

    public static final int POSITION_RIGHT = 0;
    public static final int POSITION_LEFT = 1;
    public static final int POSITION_TOP = 2;
    public static final int POSITION_BOTTOM = 3;

    public static final String[] POSITION_NAMES = {
        "Right",
        "Left",
        "Top",
        "Bottom"
    };

    public static final int POSITION_COUNT = POSITION_NAMES.length;

    public static String getPositionName(int positionIndex) {
        if (positionIndex >= 0 && positionIndex < POSITION_NAMES.length) {
            return POSITION_NAMES[positionIndex];
        }
        return POSITION_NAMES[0];
    }

    public static int getNextPosition(int currentPosition) {
        return (currentPosition + 1) % POSITION_COUNT;
    }

    public static int getPreviousPosition(int currentPosition) {
        return (currentPosition - 1 + POSITION_COUNT) % POSITION_COUNT;
    }
}
