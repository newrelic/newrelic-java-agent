/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import org.junit.Assert;

import java.util.HashMap;

public class TxTimingTest1 {
    private static final int RUNTIME_MILLIS = 5000;

    enum TestType {
        NONE, BASELINE, RESULT_LONG, RETURN_MAP, RETURN_ARRAY
    }

    // Result of operations on threads (to prevent dead code elimination)

    static volatile long resultLong;
    static volatile Object resultObject;
    static final Object anObject = new Object();

    // Three test methods. Each works in a slightly different way.

    // We disable these for commit because they take a long time and don't provide go/no go results.

    // @Test
    public void t1() throws Exception {
        doTest(TestType.BASELINE);
    }

    // @Test
    public void t2() throws Exception {
        doTest(TestType.RESULT_LONG);
    }

    // @Test
    public void t3() throws Exception {
        doTest(TestType.RETURN_MAP);
    }

    // @Test
    public void t4() throws Exception {
        doTest(TestType.RETURN_ARRAY);
    }

    // Amusingly, the Gradle support for junit apparently won't give you the standard
    // output unless their is a failure. This test satisifies its strange need for a failure.

    // @Test
    public void t5() throws Exception {
        Assert.assertFalse(true);
    }

    // Perform the test in 1 of a few slightly different variant forms.

    private void doTest(TestType how) throws Exception {
        long endMillis;
        long count = 0;

        // warm up
        endMillis = System.currentTimeMillis() + RUNTIME_MILLIS;
        while (System.currentTimeMillis() < endMillis) {
            runOneThread(how);
        }

        // run with timing
        endMillis = System.currentTimeMillis() + RUNTIME_MILLIS;
        while (System.currentTimeMillis() < endMillis) {
            runOneThread(how);
            count++;
        }

        System.out.println(count + " type \"" + how + "\" operations in " + RUNTIME_MILLIS + "ms ("
                + ((1000 * RUNTIME_MILLIS) / (double) count) + "uS/operation.)");
    }

    // Perform one test iteration (run a thread with slightly different behaviors).
    private static void runOneThread(TestType how) {
        Thread t = new TestThread(how);
        t.start();
        try {
            t.join();
        } catch (Exception ex) {
            ;
        }
    }

    private static class TestThread extends Thread {
        private final TestType how;

        public TestThread(TestType how) {
            this.how = how;
        }

        @Override
        public void run() {
            resultObject = null;
            resultLong = System.currentTimeMillis();
            if (resultLong > 42) { // always
                switch (how) {
                case NONE:
                    break;
                case BASELINE:
                    // Just put a singleton object in the result object
                    resultObject = anObject;
                    break;
                case RESULT_LONG:
                    // Allocate an HashMap() of default size and put its size in the result long
                    // The hashmap becomes garbage right now.
                    HashMap<Object, Object> foo = new HashMap<>();
                    char keys[] = new char[] { 'X', 'Y' };
                    foo.put(keys[0], resultLong);
                    if (foo.get(keys[0]) == foo.get(keys[1] - 1)) {
                        resultLong = foo.size();
                    }
                    break;
                case RETURN_MAP:
                    // Allocate an HashMap() of default size and the map itself in the result object
                    // The hashmap becomes garbage sometime later
                    resultObject = new HashMap<>();
                    break;
                case RETURN_ARRAY:
                    resultObject = new Object[100];
                    break;
                }
            }
        }
    }
}
