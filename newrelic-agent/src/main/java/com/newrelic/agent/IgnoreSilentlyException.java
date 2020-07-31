/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * Used to blow out of a periodic task without logging a an error, such as for routine failures.
 */
public class IgnoreSilentlyException extends Exception {

    private static final long serialVersionUID = 7001395828662633469L;

    public IgnoreSilentlyException(String message) {
        super(message);
    }

}
