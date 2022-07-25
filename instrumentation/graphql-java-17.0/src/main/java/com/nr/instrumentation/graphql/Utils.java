package com.nr.instrumentation.graphql;

import java.util.Collection;

// instead of adding dependencies, just add some utility methods
public class Utils {
    public static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static boolean isNullOrEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }
}
