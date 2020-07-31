/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Transaction;

public class NoOpToken implements Token {

    public static final Token INSTANCE = new NoOpToken();

    private NoOpToken() {
    }

    @Override
    public boolean expire() {
        return false;
    }

    @Override
    public boolean link() {
        return false;
    }

    @Override
    public boolean linkAndExpire() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public Transaction getTransaction() {
        return NoOpTransaction.INSTANCE;
    }

}
