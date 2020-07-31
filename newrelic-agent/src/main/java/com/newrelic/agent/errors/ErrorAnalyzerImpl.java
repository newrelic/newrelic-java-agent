/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.ExpectedErrorConfig;
import com.newrelic.agent.config.IgnoreErrorConfig;
import com.newrelic.agent.transaction.TransactionThrowable;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ErrorAnalyzerImpl implements ErrorAnalyzer {
    @VisibleForTesting
    static final Set<String> IGNORE_ERRORS;
    private final ErrorCollectorConfig errorCollectorConfig;

    static {
        Set<String> ignoreErrors = new HashSet<>(4);
        ignoreErrors.add("org.eclipse.jetty.continuation.ContinuationThrowable");
        ignoreErrors.add("org.mortbay.jetty.RetryRequest");
        IGNORE_ERRORS = Collections.unmodifiableSet(ignoreErrors);
    }

    public ErrorAnalyzerImpl(ErrorCollectorConfig errorCollectorConfig) {
        this.errorCollectorConfig = errorCollectorConfig;
    }

    @Override
    public boolean areErrorsEnabled() {
        return errorCollectorConfig.isEnabled();
    }

    @Override
    public boolean isReportable(int statusCode) {
        return statusCode >= HttpURLConnection.HTTP_BAD_REQUEST;
    }

    @Override
    public boolean isReportable(int statusCode, Throwable throwable) {
        return isReportable(statusCode) || throwable != null;
    }

    @Override
    public boolean isReportable(int statusCode, TransactionThrowable transactionThrowable) {
        return isReportable(statusCode, transactionThrowable == null ? null : transactionThrowable.throwable);
    }

    @Override
    public boolean isIgnoredError(int statusCode, Throwable throwable) {
        return isIgnoredStatus(statusCode) || isIgnoredThrowable(throwable);
    }

    @Override
    public boolean isIgnoredStatus(int statusCode) {
        return statusCode != NO_STATUS && errorCollectorConfig.getIgnoreStatusCodes().contains(statusCode);
    }

    @Override
    public boolean isIgnoredThrowable(Throwable throwable) {
        while (throwable != null) {
            String exceptionClass = throwable.getClass().getName();

            if (IGNORE_ERRORS.contains(exceptionClass)) {
                return true;
            }

            Set<IgnoreErrorConfig> ignoreErrors = this.errorCollectorConfig.getIgnoreErrors();
            for (IgnoreErrorConfig ignoreError : ignoreErrors) {
                if (ignoreError.getErrorClass().equals(exceptionClass)) {
                    String ignoreErrorMessage = ignoreError.getErrorMessage();
                    if (ignoreErrorMessage == null) {
                        return true;
                    }

                    if (insecureGetMessageNotNull(throwable).contains(ignoreErrorMessage)) {
                        return true;
                    }
                }
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    @Override
    public boolean isExpectedError(int statusCode, TransactionThrowable transactionThrowable) {
        if (transactionThrowable != null && transactionThrowable.expected) {
            return true;
        }

        if (errorCollectorConfig.getExpectedStatusCodes().contains(statusCode)) {
            return true;
        }

        Throwable throwable = transactionThrowable == null ? null : transactionThrowable.throwable;
        while (throwable != null) {
            String exceptionClass = throwable.getClass().getName();

            Set<ExpectedErrorConfig> expectedErrors = errorCollectorConfig.getExpectedErrors();
            for (ExpectedErrorConfig expectedError : expectedErrors) {
                String expectedErrorClass = expectedError.getErrorClass();
                if (exceptionClass != null && exceptionClass.equals(expectedErrorClass)) {
                    String expectedErrorMessage = expectedError.getErrorMessage();
                    if (expectedErrorMessage != null) {
                        if (insecureGetMessageNotNull(throwable).contains(expectedErrorMessage)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }

            throwable = throwable.getCause();
        }

        return false;
    }

    // Get the actual exception message, not "stripped" regardless of strip_exception_messages in the config.
    // The return value of this method MUST NOT be used for anything except matching expected or ignored errors.
    private static String insecureGetMessageNotNull(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
                ? ""
                : throwable.getMessage();
    }
}
