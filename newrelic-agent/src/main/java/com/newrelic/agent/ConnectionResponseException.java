/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent;

public class ConnectionResponseException extends Exception {
    public  ConnectionResponseException(String message) {
        super(message);
    }
}
