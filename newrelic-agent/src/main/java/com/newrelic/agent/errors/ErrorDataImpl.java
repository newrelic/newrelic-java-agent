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
        if (transactionData != null && transactionData.getThrowable() != null) {
            return transactionData.getThrowable().throwable;
        } else if (tracedError != null && tracedError instanceof ThrowableError) {
            return ((ThrowableError) tracedError).getThrowable();
        }

        return null;
    }

    @Override
    public String getErrorClass() {
        if (transactionData != null&& transactionData.getThrowable() != null && transactionData.getThrowable().throwable != null) {
            return transactionData.getThrowable().throwable.getClass().toString();
        } else if (tracedError != null && tracedError instanceof ThrowableError) {
            return ((ThrowableError) tracedError).getThrowable().getClass().toString();
        }

        return "";
    }

    @Override
    public String getErrorMessage() {
        if (transactionData != null && transactionData.getThrowable() != null && transactionData.getThrowable().throwable.getMessage() != null) {
            return transactionData.getThrowable().throwable.getMessage();
        } else if (tracedError != null && tracedError instanceof ThrowableError && ((ThrowableError) tracedError).getThrowable() != null) {
            return ((ThrowableError) tracedError).getThrowable().getMessage();
        }
        return "";
    }

    @Override
    public StackTraceElement[] getStackTraceElement() {
        if (transactionData != null && transactionData.getThrowable() != null && transactionData.getThrowable().throwable != null) {
            return transactionData.getThrowable().throwable.getStackTrace();

        } else if (tracedError != null && tracedError instanceof ThrowableError && ((ThrowableError) tracedError).getThrowable() != null) {
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
        return transactionData != null ? transactionData.getAgentAttributes().get(AttributeNames.REQUEST_URI).toString() : "";

    }

    @Override
    public String getHttpStatusCode() {
        return transactionData != null ? transactionData.getAgentAttributes().get(AttributeNames.HTTP_STATUS_CODE).toString() : "";
    }

    @Override
    public String getHttpMethod() {
        return transactionData != null ? transactionData.getAgentAttributes().get(AttributeNames.REQUEST_METHOD_PARAMETER_NAME).toString() : "";
    }

    @Override
    public boolean isErrorExpected() {
        if (transactionData != null && transactionData.getThrowable() != null) {
            return transactionData.getThrowable().expected;
        } else if (tracedError != null) {
            return tracedError.isExpected();
        }
        return false;
    }
}
