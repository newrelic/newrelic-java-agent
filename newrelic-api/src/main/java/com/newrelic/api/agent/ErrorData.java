/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.api.agent;

import java.util.Map;

/**
 * Interface that represents information about an exception to be reported to New Relic.
 * And instance of a class that implements this interface will be supplied to the
 * {@link com.newrelic.api.agent.ErrorGroupCallback ErrorGroupCallback} callback
 * registered via the {@link com.newrelic.api.agent.NewRelic#setErrorGroupCallback(ErrorGroupCallback)} setErrorGroupCallback}.
 * This callback will then generate a key that will be used to group errors in the errors inbox.
 */
public interface ErrorData {

    /**
     * Return the Throwable for the error, if available.
     *
     * @return the Throwable, if available; null otherwise
     */
    Throwable getException();

    /**
     * Return the Class of the underlying Throwable, if available; as a String
     *
     * @return the Class of the underlying Throwable as a String, null otherwise
     */
    String getErrorClass();

    /**
     * Return the error message of the reportable error
     *
     * @return The error message, null otherwise
     */
    String getErrorMessage();

    /**
     * Return an array of StackTraceElement instances, representing the stack trace of the error, if available
     *
     * @return an array of StackTraceElements, if available; null otherwise
     */
    StackTraceElement[] getStackTraceElement();

    /**
     * Return a Map instance of all the attributes associated with this caught error
     *
     * @return a Map with all attributes associated with the error
     */
    Map<String, ?> getCustomAttributes();

    /**
     * Return the transaction name, if the error was caught within a Transaction
     *
     * @return the transaction name, if available; null otherwise
     */
    String getTransactionName();

    /**
     * Return the transaction UI name, if the error was caught within a Transaction
     *
     * @return the transaction UI name, if available
     */
    String getTransactionUiName();

    /**
     * Return the request URI if the error was caught within a Transaction
     *
     * @return the request URI if available; null otherwise
     */
    String getRequestUri();

    /**
     * Return the HTTP status code as a String if the error was caught within a Transaction
     * 
     * @return the HTTP status code as a String, if available; null otherwise
     */
    String getHttpStatusCode();

    /**
     * Return the HTTP method if the error was caught within a Transaction
     *
     * @return the HTTP method, if available; null otherwise
     */
    String getHttpMethod();

    /**
     * Return true if the error was flagged as expected, false otherwise
     *
     * @return true if the error was flagged as expected, false otherwise
     */
    boolean isErrorExpected();
}
