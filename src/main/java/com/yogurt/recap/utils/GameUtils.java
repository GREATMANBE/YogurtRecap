package com.yogurt.recap.utils;

public final class GameUtils {
    private GameUtils() {}

    public static int[] getRoundTimes(int round) {
        LanguageUtils.ZombiesMap map = LanguageUtils.getMap();
        if (map == null || map == LanguageUtils.ZombiesMap.NULL) {
            return new int[0];
        }
        int[][] timer = map.getTimer();
        if (!JavaUtils.isValidIndex(timer, round - 1, 0)) {
            return new int[0];
        }
        return timer[round - 1].clone();
    }
}


