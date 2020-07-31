/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class InternalLimitExceeded extends ServerCommandException {

    public InternalLimitExceeded(String message) {
        super(message);
    }

    private static final long serialVersionUID = -6876385842601935066L;

}
