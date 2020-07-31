/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.util.Set;

import org.junit.Test;

import com.newrelic.agent.AgentHelper;
import com.newrelic.api.agent.Trace;

public class CustomBackgroundTest {

    public class CustomBackgroundDefaultTest {
        public void testDefault() {
            foo();
        }

        @Trace
        private void foo() {

        }
    }

    public class CustomBackgroundStringTest {
        public void testBackgroundString() {
            bar();
        }

        @Trace
        private void bar() {

        }
    }

    @Test
    public void test() {
        // Test that the default for `transactionType` works
        new CustomBackgroundDefaultTest().testDefault();

        Set<String> defaultMetrics = AgentHelper.getMetrics();

        AgentHelper.verifyMetrics(
                defaultMetrics,
                "OtherTransaction/all",
                "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundDefaultTest/testDefault",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundDefaultTest/testDefault",
                "Java/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundDefaultTest/testDefault",
                "Custom/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundDefaultTest/foo");

        // Test that "background" for `transactionType` works
        new CustomBackgroundStringTest().testBackgroundString();

        Set<String> backgroundStringMetrics = AgentHelper.getMetrics();

        AgentHelper.verifyMetrics(
                backgroundStringMetrics,
                "OtherTransaction/all",
                "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundStringTest/testBackgroundString",
                "Java/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundStringTest/testBackgroundString",
                "Custom/com.newrelic.agent.instrumentation.CustomBackgroundTest$CustomBackgroundStringTest/bar");
    }
}
