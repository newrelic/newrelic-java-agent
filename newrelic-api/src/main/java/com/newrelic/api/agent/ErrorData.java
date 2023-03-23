package com.newrelic.api.agent;

import java.util.Map;

public interface ErrorData {


    Exception getException();

    Class<?> getErrorClass();

    String getErrorMessage();

    //TODO FIX StackTrace
    //StackTraceElement[] getStackTrace();

    Map<String, ?> getCustomAttributes();

    String getTransactionName();

    String getTransactionUiName();

    String getRequestUri();

    String getHttpStatusCode();

    String getHttpMethod();

    String isErrorExpected();

}
