/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * An exception that forces an agent to stop reporting until its mongrel is restarted.
 */
public class ForceDisconnectException extends ServerCommandException {

    private static final long serialVersionUID = 7001395828662633469L;

    public ForceDisconnectException(String message) {
        super(message);
    }

}
