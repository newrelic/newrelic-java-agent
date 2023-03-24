package com.newrelic.api.agent;

import java.util.Map;

public interface ErrorData {


    Throwable getException();

    String getErrorClass();

    String getErrorMessage();

    //TODO FIX StackTrace
    StackTraceElement[] getStackTraceElement();

    Map<String, ?> getCustomAttributes();

    String getTransactionName();

    String getTransactionUiName();

    String getRequestUri();

    String getHttpStatusCode();

    String getHttpMethod();

    boolean isErrorExpected();

}
