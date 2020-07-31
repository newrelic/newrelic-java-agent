/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import java.net.ConnectException;

public class HostConnectException extends ConnectException {
    private final String hostName;

    public String getHostName() {
        return hostName;
    }

    public HostConnectException(String hostName, Throwable cause) {
        super(cause.getMessage());
        initCause(cause);
        this.hostName = hostName;
    }
}
