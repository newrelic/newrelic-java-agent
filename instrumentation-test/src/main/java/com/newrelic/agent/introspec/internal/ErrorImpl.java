/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.errors.HttpTracedError;
import com.newrelic.agent.errors.ThrowableError;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.introspec.Error;

class ErrorImpl implements Error {

    private Throwable throwable;
    private int statusCode;
    private String message;

    public ErrorImpl(TracedError tError) {
        message = tError.getMessage();
        if (tError instanceof ThrowableError) {
            throwable = ((ThrowableError) tError).getThrowable();
        } else if (tError instanceof HttpTracedError) {
            statusCode = ((HttpTracedError) tError).getStatusCode();
        }
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public int getResponseStatus() {
        return statusCode;
    }

    @Override
    public String getErrorMessage() {
        return message;
    }

}
