/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.HttpTestServer;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpServerRule extends ExternalResource implements HttpTestServer {
    private HttpTestServer server;

    @Override
    protected void before() throws Throwable {
        server = new HttpTestServerImpl();
    }

    @Override
    protected void after() {
        server.shutdown();
    }

    @Override
    public void shutdown() {
        server.shutdown();
    }

    @Override
    public URI getEndPoint() throws URISyntaxException {
        return server.getEndPoint();
    }

    @Override
    public String getServerTransactionName() {
        return server.getServerTransactionName();
    }

    @Override
    public String getCrossProcessId() {
        return server.getCrossProcessId();
    }

    @Override
    public void close() throws IOException {
        server.close();
    }
}
