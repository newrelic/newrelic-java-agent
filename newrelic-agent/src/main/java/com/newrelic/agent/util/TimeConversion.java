/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.concurrent.TimeUnit;

public class TimeConversion {

    public static final long NANOSECONDS_PER_SECOND = 1000000000L;
    public static final float NANOSECONDS_PER_SECOND_FLOAT = 1000000000.0F;
    public static final long MICROSECONDS_PER_SECOND = 1000000L;
    public static final long MILLISECONDS_PER_SECOND = 1000L;

    public static double convertMillisToSeconds(double millis) {
        return millis / MILLISECONDS_PER_SECOND;
    }

    public static double convertNanosToSeconds(double nanos) {
        return nanos / NANOSECONDS_PER_SECOND;
    }

    public static long convertSecondsToMillis(double seconds) {
        return (long) (seconds * MILLISECONDS_PER_SECOND); // truncate
    }

    public static long convertSecondsToNanos(double seconds) {
        return (long) (seconds * NANOSECONDS_PER_SECOND); // truncate
    }

    // convert to milliseconds with a lower bound to prevent negative or zero values where not appropriate
    public static long convertToMilliWithLowerBound(long sourceValue, TimeUnit sourceTimeUnit, long lowerBoundMilli) {
        sourceValue = TimeUnit.MILLISECONDS.convert(sourceValue, sourceTimeUnit);
        return sourceValue < lowerBoundMilli ? lowerBoundMilli : sourceValue;
    }

}
