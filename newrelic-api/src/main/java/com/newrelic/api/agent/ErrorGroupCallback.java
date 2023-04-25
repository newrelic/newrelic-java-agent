/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.api.agent;

/**
 * Classes that implement this interface are used to generate grouping keys for errors that will be sent to
 * New Relic. These keys will be used to group error messages in the Errors Inbox UI. Only one callback
 * can be registered at a time.
 * <br />
 * <br />
 * An instance of this class is registered via the {@link NewRelic#setErrorGroupCallback(ErrorGroupCallback)} method.
 * For errors that will be reported to New Relic, the generateGroupingString method is called with an instance
 * of {@link ErrorData}. The ErrorData instance contains information about the target error, such as the error message,
 * error class, transaction name, stack trace, etc (some information may not be available, depending on if the error
 * was captured in a transaction or not).
 * <br />
 * <br />
 * For example, this implementation of the generateGroupingString method will generate a key based on the error class
 * and the transaction name. If either one of those properties is empty, it will return null, which will prevent a
 * grouping key from being assigned. Note that all the methods in ErrorData will never return null, just empty Strings or
 * empty maps/arrays, except {@link ErrorData#getException()}.
 * <pre>
 * {@code
 * public class MyErrorGrouper implements ErrorGroupCallback {
 *
 *    public String generateGroupingString(ErrorData errorData) {
 *        String clazz = errorData.getErrorClass();
 *        String txnName = errorDate.getTransactionName();
 *
 *        return (clazz.isEmpty() || txnName.isEmpty()) ? null : clazz + "_" + txnName
 *    }
 * }
 * }
 * </pre>
 * To register the callback, the following would need to be called sometime early in the application startup process:
 * <pre>
 * {@code
 *     //Register error grouping key generator with the agent
 *     ErrorGroupCallback myErrorGrouper = new MyErrorGrouper();
 *     NewRelic.setErrorGroupCallback(myErrorGrouper);
 * }
 * </pre>
 */
public interface ErrorGroupCallback {

    /**
     * Method used to generate a grouping key, utilizing the data from the supplied {@link ErrorData} instance.
     *
     * @param errorData the ErrorData instance that contains information about the error that will be reported to New Relic.
     *
     * @return the key that will be used to group error messages in the Errors Inbox. If null, no grouping key will be assigned.
     */
    String generateGroupingString(ErrorData errorData);
}
