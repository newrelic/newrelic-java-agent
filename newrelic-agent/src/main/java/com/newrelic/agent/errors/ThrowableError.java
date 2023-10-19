/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.ExpectedErrorConfig;
import com.newrelic.agent.instrumentation.pointcuts.container.jetty.MultiException;
import com.newrelic.agent.util.StackTraces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ThrowableError extends TracedError {

    private final ErrorMessageReplacer errorMessageReplacer;
    private final Throwable throwable;

    protected ThrowableError(ErrorCollectorConfig errorCollectorConfig,
            ErrorMessageReplacer errorMessageReplacer,
            String appName,
            String frontendMetricName,
            String requestUri,
            Throwable error,
            long timestamp,
            Map<String, Map<String, String>> prefixedParams,
            Map<String, ?> userParams,
            Map<String, ?> agentParams,
            Map<String, ?> errorParams,
            Map<String, ?> intrinsics,
            TransactionData transactionData,
            boolean expected,
            String transactionGuid) {
        super(errorCollectorConfig, appName, frontendMetricName, timestamp, requestUri, prefixedParams, userParams,
                agentParams, errorParams, intrinsics, transactionData, expected, transactionGuid);

        this.errorMessageReplacer = errorMessageReplacer;
        this.throwable = error;
    }

    public static class Builder extends TracedError.Builder {
        private final Throwable throwable;
        private ErrorMessageReplacer errorMessageReplacer = ErrorMessageReplacer.NO_REPLACEMENT;

        Builder(ErrorCollectorConfig errorCollectorConfig, String appName, String frontendMetricName,
                Throwable throwable, long timestampInMillis) {
            super(errorCollectorConfig, appName, frontendMetricName, timestampInMillis);
            this.throwable = throwable;
        }

        public Builder errorMessageReplacer(ErrorMessageReplacer errorMessageReplacer) {
            this.errorMessageReplacer = errorMessageReplacer;
            return this;
        }

        public ThrowableError build() {
            return new ThrowableError(errorCollectorConfig, errorMessageReplacer, appName, frontendMetricName, requestUri, throwable,
                    timestampInMillis, prefixedAttributes, userAttributes, agentAttributes, errorAttributes,
                    intrinsicAttributes, transactionData, expected, transactionGuid);
        }
    }

    public static Builder builder(ErrorCollectorConfig errorCollectorConfig, String appName, String frontendMetricName,
            Throwable throwable, long timestampInMillis) {
        return new Builder(errorCollectorConfig, appName, frontendMetricName, throwable, timestampInMillis);
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String getMessage() {
        String message = errorMessageReplacer.getMessage(throwable);
        if (message == null) {
            return ErrorMessageReplacer.STRIPPED_EXCEPTION_REPLACEMENT;
        }

        return message;
    }

    @Override
    public String getExceptionClass() {
        return throwable == null ? null : throwable.getClass().getName();
    }

    @Override
    public Collection<String> stackTrace() {
        Collection<String> stackTrace = new ArrayList<>();

        if (throwable instanceof MultiException) {
            List<Throwable> throwables = ((MultiException) throwable).getThrowables();
            for (int i = 0; i < throwables.size(); i++) {
                if (i > 0) {
                    stackTrace.add(" ");
                }
                stackTrace.addAll(StackTraces.stackTracesToStrings(throwables.get(i).getStackTrace()));
            }
        } else {
            Throwable t = throwable;
            boolean inner = false;
            while (t != null) {
                if (inner) {
                    stackTrace.add(" ");
                    stackTrace.add(" caused by " + t.getClass().getName() + ": " + errorMessageReplacer.getMessage(t));
                }
                stackTrace.addAll(StackTraces.stackTracesToStrings(t.getStackTrace()));
                for (Throwable suppressed : t.getSuppressed()) {
                    stackTrace.add("\tSuppressed: " + suppressed.getClass().getName() + ": " + errorMessageReplacer.getMessage(suppressed));
                    stackTrace.addAll(
                            StackTraces.stackTracesToStrings(suppressed.getStackTrace())
                                .stream().map(line -> "\t\t" + line)
                                .collect(Collectors.toCollection(ArrayList::new))
                    );
                }
                t = t.equals(t.getCause()) ? null : t.getCause();
                inner = true;
            }
        }

        return stackTrace;
    }

    @Override
    public boolean incrementsErrorMetric() {
        // noticeError(expected = true)?
        if (expected) {
            return false;
        }

        boolean shouldIncrement = true;

        String exceptionClass = getExceptionClass();
        String exceptionMessage = getMessage();

        Set<ExpectedErrorConfig> expectedErrors = errorCollectorConfig.getExpectedErrors();
        for (ExpectedErrorConfig expectedError : expectedErrors) {
            String expectedErrorClass = expectedError.getErrorClass();
            if (exceptionClass != null && exceptionClass.equals(expectedErrorClass)) {
                String expectedErrorMessage = expectedError.getErrorMessage();
                if (expectedErrorMessage != null) {
                    if (exceptionMessage != null && exceptionMessage.contains(expectedErrorMessage)) {
                        shouldIncrement = false;
                        break;
                    }
                } else {
                    shouldIncrement = false;
                    break;
                }
            }
        }

        return shouldIncrement;
    }

    @Override
    public String toString() {
        return getMessage();
    }

    @Override
    public int hashCode() {
        return throwable.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ThrowableError other = (ThrowableError) obj;
        return throwable.equals(other.throwable);
    }
}
