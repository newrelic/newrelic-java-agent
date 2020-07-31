/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

public class ReportableError extends Throwable {
    private static final long serialVersionUID = 3472056044517410355L;

    public ReportableError(String message) {
        super(message);
    }
}
