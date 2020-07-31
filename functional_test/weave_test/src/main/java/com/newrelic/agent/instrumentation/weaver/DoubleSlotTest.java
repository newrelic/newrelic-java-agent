/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class DoubleSlotTest {

    public int addInts(int i, long l, int i2, long l2, String s) {
        int orig = Weaver.callOriginal();
        return orig+1;
    }

    public static int staticAddInts(int i, long l, int i2, long l2, String s) {
        int orig = Weaver.callOriginal();
        return orig+1;
    }
    
    ///
    
    public long addLongs(int i, long l, int i2, long l2, String s) {
        long orig = Weaver.callOriginal();
        return orig+1L;
    }

    public static long staticAddLongs(int i, long l, int i2, long l2, String s) {
        long orig = Weaver.callOriginal();
        return orig+1L;
    }
    
    ///
    
    public double addDoubles(int i, double d, int i2, double d2, String s) {
        double orig = Weaver.callOriginal();
        return orig+1d;
    }

    public static double staticAddDoubles(int i, double d, int i2, double d2, String s) {
        double orig = Weaver.callOriginal();
        return orig+1d;
    }

}
