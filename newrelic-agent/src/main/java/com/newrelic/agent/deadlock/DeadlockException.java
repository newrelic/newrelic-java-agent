/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.deadlock;

public class DeadlockException extends Exception {
    public DeadlockException(String message) {
        super(message);
    }
}
