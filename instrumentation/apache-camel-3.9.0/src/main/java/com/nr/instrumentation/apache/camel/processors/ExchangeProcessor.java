/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.Transaction;
import org.apache.camel.Exchange;

public interface ExchangeProcessor {
    void nameTransaction(Transaction transaction, Exchange exchange);
    boolean shouldStartTransaction();
    void processInbound(Transaction txn, Exchange exchange);
}
