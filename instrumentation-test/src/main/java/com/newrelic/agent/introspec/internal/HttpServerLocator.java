/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.io.IOException;

import com.newrelic.agent.introspec.HttpTestServer;

public class HttpServerLocator {
    public static HttpTestServer createAndStart() throws IOException {
        return new HttpTestServerImpl();
    }
}
