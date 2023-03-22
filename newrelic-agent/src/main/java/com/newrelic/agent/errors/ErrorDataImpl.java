package com.newrelic.agent.errors;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ErrorData;
import jdk.jfr.StackTrace;

import java.util.Map;

public class ErrorDataImpl implements ErrorData {

    private TracedError tracedError;
    private Transaction tx;

    public ErrorDataImpl(TracedError tracedError, Transaction tx) {
        this.tracedError = tracedError;
        this.tx = tx;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public Class<?> getErrorClass() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public StackTrace getStackTrace() {
        return null;
    }

    @Override
    public Map<String, ?> getCustomAttributes() {
        return null;
    }

    @Override
    public String getTransactionName() {
        return null;
    }

    @Override
    public String getTransactionUiName() {
        return null;
    }

    @Override
    public String getRequestUri() {
        return null;
    }

    @Override
    public String getHttpStatusCode() {
        return null;
    }

    @Override
    public String getHttpMethod() {
        return null;
    }

    @Override
    public String isErrorExpected() {
        return null;
    }
}
