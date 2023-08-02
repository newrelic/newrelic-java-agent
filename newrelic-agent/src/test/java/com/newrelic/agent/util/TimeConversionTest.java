package com.newrelic.agent.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TimeConversionTest {
    @Test
    public void convertMillisToSeconds() {
        assertEquals(10.0, TimeConversion.convertMillisToSeconds(10000.0), 0.1);
    }

    @Test
    public void convertNanosToSeconds() {
        assertEquals(1.0, TimeConversion.convertNanosToSeconds(1000000000.0), 0.1);
    }

    @Test
    public void convertSecondsToMillis() {
        assertEquals(10000.0, TimeConversion.convertSecondsToMillis(10.0), 0.1);
    }

    @Test
    public void convertSecondsToNanos() {
        assertEquals(1000000000.0, TimeConversion.convertSecondsToNanos(1.0), 0.1);
    }

    @Test
    public void convertToMilliWithLowerBound() {
        assertEquals(10000.0, TimeConversion.convertToMilliWithLowerBound(10, TimeUnit.SECONDS, 1), 0.1);
    }
}
