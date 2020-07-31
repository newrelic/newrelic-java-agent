/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * An exception that forces an agent to restart.
 */
public class ForceRestartException extends ServerCommandException {

    private static final long serialVersionUID = 7001395828662633469L;

    public ForceRestartException(String message) {
        super(message);
    }

}
