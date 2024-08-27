/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.messaging;

public class BrokerInstance {
    private final String hostName;
    private final Integer port;

    public static BrokerInstance empty() {
        return new BrokerInstance(null, null);
    }

    public BrokerInstance(String host, Integer port) {
        this.hostName = host;
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public Integer getPort() {
        return port;
    }
}