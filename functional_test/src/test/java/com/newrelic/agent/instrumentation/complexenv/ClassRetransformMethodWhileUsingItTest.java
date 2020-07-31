/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.complexenv;

import java.util.Arrays;

import org.junit.Test;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;

public class ClassRetransformMethodWhileUsingItTest {

    public class TheObject extends Thread {

        private boolean isShutdown = false;

        @Override
        public void run() {
            while (!isShutdown) {
                performWork();
            }
        }

        public void setIsShutdown(boolean pShutdown) {
            isShutdown = pShutdown;
        }

        public void performWork() {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                // do nothing here
            }
        }
    }

    @Test
    public void testMethodWhileUsingIt() throws Exception {
        String className = "com.newrelic.agent.instrumentation.complexenv.ClassRetransformMethodWhileUsingItTest$TheObject";
        String transMetric = InstrumentTestUtils.TRANS_PREFIX + className + "/performWork";
        String methodMetric = InstrumentTestUtils.METHOD_PREFIX + className + "/performWork";

        TheObject obj = new TheObject();
        obj.start();

        sleepSome();
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transMetric, methodMetric));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "performWork", "()V");

        sleepSome();

        InstrumentTestUtils.verifyMetricPresent(transMetric, methodMetric);

    }

    private void sleepSome() {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // do nothing here
        }
    }

}
