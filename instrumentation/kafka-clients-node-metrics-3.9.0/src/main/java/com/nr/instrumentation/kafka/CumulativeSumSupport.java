package com.nr.instrumentation.kafka;

public class CumulativeSumSupport {
    private static final String CUMULATIVE_SUM_CLASS = "org.apache.kafka.common.metrics.stats.CumulativeSum";

    private static final boolean SUPPORTS_CUMULATIVE_SUM;
    static {
        boolean supportsCumulativeSum;
        try {
            Class.forName(CUMULATIVE_SUM_CLASS);
            supportsCumulativeSum = true;
        } catch (final ClassNotFoundException e) {
            supportsCumulativeSum = false;
        }
        SUPPORTS_CUMULATIVE_SUM = supportsCumulativeSum;
    }

    public static boolean isCumulativeSumClass(String className) {
        // only do the string comparison if the system has the cumulative sum class
        return SUPPORTS_CUMULATIVE_SUM && CUMULATIVE_SUM_CLASS.equals(className);
    }

    public static boolean isCumulativeSumSupported() {
        return SUPPORTS_CUMULATIVE_SUM;
    }
}
