/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.TransactionErrorPriority;

public interface TransactionErrorTracker {

    TransactionThrowable getThrowable();

    void setThrowable(TransactionThrowable transactionThrowable);

    void setThrowable(Throwable throwable, TransactionErrorPriority priority, boolean expected, String mostRecentSpanId);

    boolean tryUpdatePriority(TransactionErrorPriority newPriority);

    TransactionErrorPriority getPriority();

    void noticeTracerException(Throwable throwable, String spanId);
}
