/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;

import com.newrelic.agent.introspec.internal.HttpServerLocator;

/**
 * This can be used to test external calls with or without cat.
 * 
 * In a unit test, get the HttpTestServer from the {@link HttpServerLocator} using the createAndStart method. Get URI
 * for your connection using getEndPoint. Then make your request to that endpoint. In order for CAT to be successful,
 * you must make the appropriate CAT calls like you are instrumenting an external request in the framework or your test.
 */
public interface HttpTestServer extends Closeable {
    /**
     * Set this header to cause a pause for the amount of ms in the value.
     */
    String SLEEP_MS_HEADER_KEY = "sleep-time";

    /**
     * By default the server will reply with the proper cat headers.
     * Set this header to false to disable cat headers in the response.<br/>
     * This is useful for testing external (non-cat) http servers.
     */
    String DO_CAT = "do-cat";

    /**
     * By default the server will not attempt to read better cat headers.
     * Set this header to true to enable better cat header parsing.
     */
    String DO_BETTER_CAT = "do-better-cat";

    /**
     * Set this url parameter to *not* start a transaction within the test server
     */
    String NO_TRANSACTION = "no-transaction";

    String DISTRIBUTED_TRACE_ENDPOINTS = "distributed-trace-endpoints";
    
    void shutdown();

    URI getEndPoint() throws URISyntaxException;

    String getServerTransactionName();

    String getCrossProcessId();

}
