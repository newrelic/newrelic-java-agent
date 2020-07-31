/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class InfiniteTracingTraceObserverConfig extends BaseConfig {
    public static final String ROOT = "trace_observer";
    public static final String HOST = "host";
    public static final String PORT = "port";

    public static final String DEFAULT_HOST = "";
    public static final int DEFAULT_PORT = 443;

    private final String host;
    private final int port;

    public InfiniteTracingTraceObserverConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");
        host = getProperty(HOST, DEFAULT_HOST);
        port = getIntProperty(PORT, DEFAULT_PORT);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
