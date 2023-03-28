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
        return tracedError instanceof ThrowableError ? ((ThrowableError) tracedError).getThrowable() : null;
    }

    @Override
    public String getErrorClass() {
        return transactionData != null ? transactionData.getThrowable().getClass().toString() : null;
    }

    @Override
    public String getErrorMessage() {
        if (transactionData != null && transactionData.getThrowable() != null && transactionData.getThrowable().throwable.getMessage() != null) {
            return transactionData.getThrowable().throwable.getMessage();
        } else if (tracedError != null && tracedError instanceof ThrowableError && ((ThrowableError) tracedError).getThrowable() != null) {
            return ((ThrowableError) tracedError).getThrowable().getMessage();
        }
        return null;
    }

    @Override
    public StackTraceElement[] getStackTraceElement() {
        if (transactionData != null && transactionData.getThrowable() != null && transactionData.getThrowable().throwable != null) {
            return transactionData.getThrowable().throwable.getStackTrace();

        } else if (tracedError != null && tracedError instanceof ThrowableError && ((ThrowableError) tracedError).getThrowable() != null) {
            return ((ThrowableError) tracedError).getThrowable().getStackTrace();

        }
        return null;
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
        return transactionData.getTransaction().getPriorityTransactionName().getName();
    }

    @Override
    public String getTransactionUiName() {
        return getTransactionName();
    }
    @Override
    public String getRequestUri() {
        return transactionData != null ? transactionData.getAgentAttributes().get(AttributeNames.REQUEST_URI).toString() : null;

    }

    @Override
    public String getHttpStatusCode() {
        return transactionData != null ? transactionData.getAgentAttributes().get(AttributeNames.HTTP_STATUS_CODE).toString() : null;
    }

    @Override
    public String getHttpMethod() {
        return transactionData != null ? transactionData.getAgentAttributes().get(AttributeNames.REQUEST_METHOD_PARAMETER_NAME).toString() : null;
    }

    @Override
    public boolean isErrorExpected() {
        return tracedError != null && tracedError.isExpected();
    }
}
