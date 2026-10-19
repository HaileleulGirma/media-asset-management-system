package com.ena.mam.util;

public final class DurationUtils {

    private DurationUtils() {
        // Utility class; prevent instantiation
    }

    /**
     * Converts hours, minutes, and seconds into total seconds.
     * Treats null inputs as 0.
     */
    public static int toTotalSeconds(Integer hours, Integer minutes, Integer seconds) {
        int h = hours != null ? hours : 0;
        int m = minutes != null ? minutes : 0;
        int s = seconds != null ? seconds : 0;

        return (h * 3600) + (m * 60) + s;
    }
}