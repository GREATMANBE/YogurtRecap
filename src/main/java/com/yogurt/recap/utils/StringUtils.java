package com.yogurt.recap.utils;

import java.util.regex.Pattern;

public final class StringUtils {
    private StringUtils() {}

    private static final Pattern MC_FORMATTING_CODE = Pattern.compile("§.");

    public static String trim(String s) {
        if (s == null) {
            return "";
        }
        return MC_FORMATTING_CODE.matcher(s).replaceAll("").trim();
    }

    public static int getNumberInString(String s) {
        if (s == null) {
            return 0;
        }
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}


