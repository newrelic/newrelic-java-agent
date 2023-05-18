/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.config.StripExceptionConfig;

import java.util.Set;

public class ErrorMessageReplacer {
    @VisibleForTesting
    static final String STRIPPED_EXCEPTION_REPLACEMENT = "Message removed by New Relic 'strip_exception_messages' setting";
    private final StripExceptionConfig config;

    public ErrorMessageReplacer(StripExceptionConfig config) {
        this.config = config;
    }

    /**
     * Decides if the exception message needs replacement.
     *
     * @param throwableParam The exception to inspect.
     * @return {@literal null} if the message needs replacement, {@literal ""} if the exception or its message are null, and the exception message otherwise.
     */
    public String getMessage(Throwable throwableParam) {
        String result;
        if (throwableParam == null) {
            return "";
        }

        if (useStrippedExceptionReplacement(throwableParam)) {
            return STRIPPED_EXCEPTION_REPLACEMENT;
        }

        result = throwableParam.getMessage();
        if (result == null) {
            return "";
        }

        return result;
    }

    boolean useStrippedExceptionReplacement(Throwable throwable) {
        return config != null
                && config.isEnabled()
                && !config.getAllowedClasses().contains(throwable.getClass().getName());
    }

    @VisibleForTesting
    public static final ErrorMessageReplacer NO_REPLACEMENT = new ErrorMessageReplacer(new StripExceptionConfig() {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public Set<String> getAllowedClasses() {
            return null;
        }
    });
}