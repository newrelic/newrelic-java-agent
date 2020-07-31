/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class MaxPayloadException extends Exception {

    public MaxPayloadException(String message) {
        super(message);
    }
}
