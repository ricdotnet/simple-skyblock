package dev.ricr.skyblock.utils;

public class NumberUtils {
    public static double objectToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        return Double.NaN;
    }

    public static float objectToFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        return Float.NaN;
    }
}
