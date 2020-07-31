/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class MockDispatcher implements Dispatcher {

    private boolean isWebTransaction = false;
    private String uri;

    @Override
    public void setTransactionName() {
    }

    @Override
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
       this.uri = uri;
    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public Response getResponse() {
        return null;
    }

    @Override
    public void setRequest(Request request) {
    }

    @Override
    public void setResponse(Response response) {
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig() {
        return null;
    }

    @Override
    public boolean isWebTransaction() {
        return isWebTransaction;
    }

    public void setWebTransaction(boolean isWebTransaction) {
        this.isWebTransaction = isWebTransaction;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    public final void setIgnoreApdex(boolean ignore) {
    }

    @Override
    public boolean isIgnoreApdex() {
        return false;
    }

    @Override
    public void transactionFinished(String transactionName, TransactionStats stats) {

    }

    @Override
    public void transactionActivityWithResponseFinished() {

    }

}
