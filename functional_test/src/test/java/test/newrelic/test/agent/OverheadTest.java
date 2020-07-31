/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.api.agent.Trace;

public class OverheadTest {

    private final int ITERATION_COUNT = 50000;

    @Test
    public void time() throws Exception {
        Simple object = new Simple();
        object.instrumented(); // warm up

        long now = System.currentTimeMillis();
        for (int i = 0; i < ITERATION_COUNT; i++) {
            object.uninstrumented();
        }
        long uninstrumentedTime = System.currentTimeMillis() - now;

        now = System.currentTimeMillis();
        for (int i = 0; i < ITERATION_COUNT; i++) {
            object.instrumented();
        }
        long instrumentedTime = System.currentTimeMillis() - now;

        System.currentTimeMillis();
        object.nested();
        long nestedTime = System.currentTimeMillis() - now;

        System.out.println(ITERATION_COUNT + " uninstrumented method calls took " + uninstrumentedTime + "ms");
        System.out.println(ITERATION_COUNT + " instrumented method calls took " + instrumentedTime + "ms");
        double singleInvocationOverhead = (double) instrumentedTime / (double) ITERATION_COUNT;
        System.out.println("Agent overhead per invocation: " + singleInvocationOverhead + "ms");

        System.out.println(ITERATION_COUNT + " instrumented method calls nested within another instrumented call took "
                + nestedTime + "ms");

        singleInvocationOverhead = (double) nestedTime / (double) (ITERATION_COUNT + 1);
        System.out.println("Agent overhead per invocation: " + singleInvocationOverhead + "ms");

        Assert.assertTrue("Our overhead of " + singleInvocationOverhead + " is greater than 2 millis",
                singleInvocationOverhead < 2);
    }

    private class Simple {
        @Trace
        public void instrumented() throws InterruptedException {
        }

        public void uninstrumented() throws InterruptedException {
        }

        @Trace
        public void nested() throws InterruptedException {
            for (int i = 0; i < ITERATION_COUNT; i++) {
                instrumented();
            }
        }

    }
}
