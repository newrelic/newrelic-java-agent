/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * Used for when a transaction trace or error report has too much data, so we reset the queue to clear the extra-large
 * item
 */
public class PostTooBigException extends IgnoreSilentlyException {

    private static final long serialVersionUID = 7001395828662633469L;

    public PostTooBigException(String message) {
        super(message);
    }

}
