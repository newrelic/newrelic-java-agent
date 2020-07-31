/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

public class InvalidMethodDescriptor extends Exception {

    private static final long serialVersionUID = 2591402822329628860L;

    public InvalidMethodDescriptor(String message) {
        super(message);
    }

}
