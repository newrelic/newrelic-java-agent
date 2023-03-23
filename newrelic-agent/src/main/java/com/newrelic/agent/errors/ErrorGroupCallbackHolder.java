/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.errors;

import com.newrelic.api.agent.ErrorData;
import com.newrelic.api.agent.ErrorGroupCallback;

public class ErrorGroupCallbackHolder {

    private static ErrorGroupCallback NO_OP_INSTANCE = new NoOpErrorGroupCallback();
    private static ErrorGroupCallback errorGroupCallback = NO_OP_INSTANCE;

    public static void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        errorGroupCallback = errorGroupCallback == null ? NO_OP_INSTANCE : errorGroupCallback;
    }

    public static ErrorGroupCallback getErrorGroupCallback() {
        return errorGroupCallback;
    }

    private static class NoOpErrorGroupCallback implements ErrorGroupCallback {
        @Override
        public String generateGroupingString(ErrorData errorData) {
            return null;
        }
    }
}
