/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * An exception that allows the server to tell our agent to take an action.
 */
public class ServerCommandException extends Exception {

    private static final long serialVersionUID = 7001395828662633469L;

    public ServerCommandException(String message) {
        super(message);
    }

}
