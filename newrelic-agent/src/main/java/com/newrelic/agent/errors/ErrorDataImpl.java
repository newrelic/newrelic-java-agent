package com.newrelic.agent.errors;

import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.api.agent.ErrorData;

import java.util.Map;

public class ErrorDataImpl implements ErrorData {

    private final ErrorEvent errorEvent;
    private final TracedError tracedError;

    //TODO FIX
    public ErrorDataImpl(ErrorEvent errorEvent, TracedError tracedError) {
        this.errorEvent = errorEvent;
        this.tracedError = tracedError;
    }


    //TODO is this return type change ok?
    @Override
    public Throwable getException() {
        return tracedError instanceof ThrowableError ? ((ThrowableError) tracedError).getThrowable() : null;
    }

    //TODO any issue with changing to a String instead of Class<?>
    @Override
    public String getErrorClass() {
        return errorEvent != null ? errorEvent.getErrorClass() : null;
    }

    @Override
    public String getErrorMessage() {
        return errorEvent != null ? errorEvent.getErrorMessage() : null;
    }

    @Override
    public StackTraceElement[] getStackTraceElement() {
        return tracedError instanceof ThrowableError ? ((ThrowableError) tracedError).getThrowable().getStackTrace() : null;
    }

    //TODO we also need the other User Attributes here according to the spec, so we may need to merge a map, but there might be some issues getting
    // i.e merging a map and high security mode?
    @Override
    public Map<String, ?> getCustomAttributes() {
        return tracedError.getErrorAtts();
    }

    @Override
    public String getTransactionName() {
        return errorEvent.getTransactionName();
    }

    @Override
    public String getTransactionUiName() {
        return getTransactionName();
    }

    //TODO questions on how to handle Object
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
    public boolean isErrorExpected() {
        return tracedError.isExpected();
    }
}
