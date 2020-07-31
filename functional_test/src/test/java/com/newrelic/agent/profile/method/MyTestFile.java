/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import com.newrelic.api.agent.Trace;

public class MyTestFile {

    @Trace(dispatcher = true)
    StackTraceElement[] bar() {
        return Thread.currentThread().getStackTrace();
    }

    @Trace(dispatcher = false)
    StackTraceElement[] bar(int value) {
        return Thread.currentThread().getStackTrace();
    }

    @Trace(skipTransactionTrace = true)
    StackTraceElement[] bar(long value) {
        return Thread.currentThread().getStackTrace();
    }

    @Trace(dispatcher = false)
    StackTraceElement[] bar(String value) {
        return Thread.currentThread().getStackTrace();
    }

    StackTraceElement[] bar(String value1, Object value2) {
        return Thread.currentThread().getStackTrace();
    }

    StackTraceElement[] bar(int[] one, String[][] two, Object[][][] three) {
        return Thread.currentThread().getStackTrace();
    }

}
