/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

/**
 * Longs and doubles take up two slots in bytecode. This requires testing to make sure those special cases are handled.
 */
public class DoubleSlotTest {

    // ints

    public int addInts(int i, long l, int i2, long l2, String s) {
        int result = i + i2;
        return result;
    }

    public static int staticAddInts(int i, long l, int i2, long l2, String s) {
        int result = i + i2;
        return result;
    }

    // longs

    public long addLongs(int i, long l, int i2, long l2, String s) {
        long result = l + l2;
        return result;
    }

    public static long staticAddLongs(int i, long l, int i2, long l2, String s) {
        long result = l + l2;
        return result;
    }

    // doubles

    public double addDoubles(int i, double d, int i2, double d2, String s) {
        double result = d + d2;
        return result;
    }

    public static double staticAddDoubles(int i, double d, int i2, double d2, String s) {
        double result = d + d2;
        return result;
    }
}
