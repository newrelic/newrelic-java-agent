/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.errors;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.api.agent.ErrorData;

import java.util.HashMap;
import java.util.Map;

public class ErrorDataImpl implements ErrorData {

    private final TransactionData transactionData;
    private final TracedError tracedError;

    public ErrorDataImpl(TransactionData txData, TracedError tracedError) {
        this.transactionData = txData;
        this.tracedError = tracedError;
    }

    @Override
    public Throwable getException() {
        if (isTransactionDataThrowableValid()) {
            return transactionData.getThrowable().throwable;
        } else if (isTracedErrorInstanceOfThrowableError()) {
            return ((ThrowableError) tracedError).getThrowable();
        }

        return null;
    }

    @Override
    public String getErrorClass() {
        if (isTransactionDataThrowableValid() && transactionData.getThrowable().throwable != null) {
            return transactionData.getThrowable().throwable.getClass().toString();
        } else if (isTracedErrorInstanceOfThrowableError()) {
            return ((ThrowableError) tracedError).getThrowable().getClass().toString();
        }

        return "";
    }

    @Override
    public String getErrorMessage() {
        if (isTransactionDataThrowableValid() && transactionData.getThrowable().throwable.getMessage() != null) {
            return transactionData.getThrowable().throwable.getMessage();
        } else if (isTracedErrorInstanceOfThrowableError() && ((ThrowableError) tracedError).getThrowable() != null) {
            return ((ThrowableError) tracedError).getThrowable().getMessage();
        }
        return "";
    }

    @Override
    public StackTraceElement[] getStackTraceElement() {
        if (isTransactionDataThrowableValid() && transactionData.getThrowable().throwable != null) {
            return transactionData.getThrowable().throwable.getStackTrace();

        } else if (isTracedErrorInstanceOfThrowableError() && ((ThrowableError) tracedError).getThrowable() != null) {
            return ((ThrowableError) tracedError).getThrowable().getStackTrace();

        }
        return new StackTraceElement[] {};
    }

    @Override
    public Map<String, ?> getCustomAttributes() {
        Map<String, ? super Object> combinedUserAttMap = new HashMap<>();
        if (transactionData != null) {
            if (transactionData.getUserAttributes() != null) {
                combinedUserAttMap.putAll(transactionData.getUserAttributes());
            }
            if (transactionData.getErrorAttributes() != null) {
                combinedUserAttMap.putAll(transactionData.getErrorAttributes());
            }
        }
        if (tracedError != null && tracedError.getErrorAtts() != null) {
            combinedUserAttMap.putAll(tracedError.getErrorAtts());
        }

        return combinedUserAttMap;
    }

    @Override
    public String getTransactionName() {
        return transactionData != null ? transactionData.getTransaction().getPriorityTransactionName().getName() : "";
    }

    @Override
    public String getTransactionUiName() {
        return getTransactionName();
    }

    @Override
    public String getRequestUri() {
        return getAgentAttributeOrReturnEmptyString(AttributeNames.REQUEST_URI);
    }

    @Override
    public String getHttpStatusCode() {
        return getAgentAttributeOrReturnEmptyString(AttributeNames.HTTP_STATUS_CODE);
    }

    @Override
    public String getHttpMethod() {
        return getAgentAttributeOrReturnEmptyString(AttributeNames.REQUEST_METHOD_PARAMETER_NAME);
    }

    @Override
    public boolean isErrorExpected() {
        if (isTransactionDataThrowableValid()) {
            return transactionData.getThrowable().expected;
        } else if (tracedError != null) {
            return tracedError.isExpected();
        }
        return false;
    }

    private boolean isTransactionDataThrowableValid() {
        return transactionData != null && transactionData.getThrowable() != null;
    }

    private boolean isTracedErrorInstanceOfThrowableError() {
        return tracedError != null && tracedError instanceof ThrowableError;
    }

    private String getAgentAttributeOrReturnEmptyString(String key) {
        Object value = null;
        if (transactionData != null) {
            value = transactionData.getAgentAttributes().get(key);
        }

        return value == null ? "" : value.toString();
    }
}
