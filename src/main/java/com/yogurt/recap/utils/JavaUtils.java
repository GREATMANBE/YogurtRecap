package com.yogurt.recap.utils;

public final class JavaUtils {
    private JavaUtils() {}

    public static boolean isValidIndex(int[][] array, int row, int col) {
        int totalRows = array.length;
        if (row < 0 || row >= totalRows) {
            return false;
        }
        int totalCols = array[row].length;
        return col >= 0 && col < totalCols;
    }

    public static boolean isValidIndex(int[] array, int row) {
        int totalRows = array.length;
        return row >= 0 && row < totalRows;
    }

    /**
     * Returns the index where {@code target} would be inserted to keep {@code array} sorted.
     * Equivalent to lower_bound.
     */
    public static int findInsertPosition(int[] array, int target) {
        int left = 0;
        int right = array.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (array[mid] == target) {
                return mid;
            } else if (array[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return left;
    }
}


