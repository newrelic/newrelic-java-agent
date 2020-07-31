/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

public class TransactionThrowable {
    public final Throwable throwable;
    public final boolean expected;
    public final String spanId;

    /**
     * @param throwable a throwable. Must never be null.
     * @param expected whether this throwable is an expected error.
     * @param spanId the span in which this error occurred.
     */
    public TransactionThrowable(Throwable throwable, boolean expected, String spanId) {
        this.throwable = throwable;
        this.expected = expected;
        this.spanId = spanId;
    }
}
