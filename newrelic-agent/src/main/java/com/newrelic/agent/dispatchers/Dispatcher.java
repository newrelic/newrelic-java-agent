/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.dispatchers;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

/**
 * Provides information about the dispatcher for a transaction. The dispatcher is also responsible for generating the
 * metrics for the root tracer including any rollup metrics like HttpDispatcher or OtherTransaction/all.
 */
public interface Dispatcher {

    void setTransactionName();

    String getUri();

    /**
     * Different dispatchers have different TT configurations.
     * 
     * @see AgentConfig#getRequestTransactionTracerConfig()
     * @see AgentConfig#getBackgroundTransactionTracerConfig()
     */
    TransactionTracerConfig getTransactionTracerConfig();

    boolean isWebTransaction();

    void transactionFinished(String transactionName, TransactionStats stats);

    void transactionActivityWithResponseFinished();

    String getCookieValue(String name);

    String getHeader(String name);

    Request getRequest();

    void setRequest(Request request);

    Response getResponse();

    void setResponse(Response response);

    void setIgnoreApdex(boolean ignore);

    boolean isIgnoreApdex();
}
