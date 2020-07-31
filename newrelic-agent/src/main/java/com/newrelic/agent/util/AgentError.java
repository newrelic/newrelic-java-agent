/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

public class AgentError extends Error {

    private static final long serialVersionUID = -2870952056899794642L;

    public AgentError(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentError(String message) {
        super(message);
    }

    public AgentError(Throwable cause) {
        super(cause);
    }

}
