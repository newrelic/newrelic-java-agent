/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.service.slowtransactions.SlowTransactionService;

public interface SlowTransactionsConfig {

    /**
     * True if the {@link SlowTransactionService} is enabled, else false.
     */
    boolean isEnabled();

    /**
     * The minimum number of milliseconds a transaction must be running to be
     * reported as slow.
     */
    long getThresholdMillis();

    /**
     * By default, only in-progress transactions are checked for exceeding the defined
     * threshold time during a normal harvest cycle. Setting this to true will
     * evaluate transactions as they complete and report any that exceed the defined
     * threshold.
     */
    boolean evaluateCompletedTransactions();

}
