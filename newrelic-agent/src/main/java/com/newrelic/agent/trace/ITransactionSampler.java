/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import java.util.List;

import com.newrelic.agent.TransactionData;

public interface ITransactionSampler {

    boolean noticeTransaction(TransactionData td);

    List<TransactionTrace> harvest(String appName);

    void stop();

}
