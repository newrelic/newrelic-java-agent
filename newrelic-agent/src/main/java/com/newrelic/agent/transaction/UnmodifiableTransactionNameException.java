/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

public class UnmodifiableTransactionNameException extends Exception {

    private static final long serialVersionUID = 2277591207140681026L;

    public UnmodifiableTransactionNameException(Exception ex) {
        super(ex);
    }

}
