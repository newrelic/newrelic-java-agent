package com.newrelic.api.agent;

import jdk.jfr.StackTrace;

import java.util.Map;

public interface ErrorData {


    Exception getException();

    Class<?> getErrorClass();

    String getErrorMessage();

    StackTrace getStackTrace();

    Map<String, ?> getCustomAttributes();

    String getTransactionName();

    String getTransactionUiName();

    String getRequestUri();

    String getHttpStatusCode();

    String getHttpMethod();

    String isErrorExpected();

}
