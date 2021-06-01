/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

import java.util.Map;

/**
 * The public api interface.
 */
public interface PublicApi {

    // ************************** Error collector ***********************************//

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * @param throwable The throwable to notice and report.
     * @param params Custom parameters to include in the traced error. May be null.
     */
    void noticeError(Throwable throwable, Map<String, ?> params);

    /**
     * Report an exception to New Relic.
     *
     * @param throwable The throwable to report.
     * @see #noticeError(Throwable, Map)
     */
    void noticeError(Throwable throwable);

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * @param message The error message to be reported.
     * @param params Custom parameters to include in the traced error. May be null.
     */
    void noticeError(String message, Map<String, ?> params);

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * @param message Message to report with a transaction when it finishes.
     */
    void noticeError(String message);

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param throwable The throwable to notice and report.
     * @param params Custom parameters to include in the traced error. May be null.
     * @param expected true if this error is expected, false otherwise.
     */
    void noticeError(Throwable throwable, Map<String, ?> params, boolean expected);

    /**
     * Report an exception to New Relic.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param throwable The throwable to report.
     * @param expected true if this error is expected, false otherwise.
     * @see #noticeError(Throwable, Map)
     */
    void noticeError(Throwable throwable, boolean expected);

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param message The error message to be reported.
     * @param params Custom parameters to include in the traced error. May be null.
     * @param expected true if this error is expected, false otherwise.
     */
    void noticeError(String message, Map<String, ?> params, boolean expected);

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param message Message to report with a transaction when it finishes.
     * @param expected true if this error is expected, false otherwise.
     */
    void noticeError(String message, boolean expected);

    // **************************** Transaction APIs ********************************//

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     */
    void addCustomParameter(String key, Number value);

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     */
    void addCustomParameter(String key, String value);

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     */
    void addCustomParameter(String key, boolean value);

    /**
     * Add key/value pairs to the current transaction. These are reported in errors and transaction traces.
     *
     * @param params Custom parameters to include.
     */
    void addCustomParameters(Map<String, Object> params);

    /**
     * Set the name of the current transaction.
     *
     * @param category Metric category. If the input is null, then the default will be used.
     * @param name The name of the transaction starting with a forward slash. example: /store/order
     */
    void setTransactionName(String category, String name);

    /**
     * Ignore the current transaction.
     */
    void ignoreTransaction();

    /**
     * Ignore the current transaction for calculating Apdex score.
     */
    void ignoreApdex();

    /**
     * Sets the request and response instances for the current transaction. Use this API to generate web transactions
     * for custom web request dispatchers. Only the first call to setRequestAndResponse will take effect.
     *
     * @param request The current transaction's request.
     * @param response The current transaction's response.
     */
    void setRequestAndResponse(Request request, Response response);

    // **************************** Real User Monitoring (RUM) ********************************
    // API calls to support manual instrumentation of RUM if auto instrumentation is not available.
    // Get the JavaScript header and footer that should be inserted into the HTML response for browser-side monitoring.

    /**
     * Get the RUM JavaScript header for the current web transaction.
     *
     * @return RUM JavaScript header for the current web transaction.
     */
    String getBrowserTimingHeader();

    /**
     * Get the RUM JavaScript footer for the current web transaction.
     *
     * @return RUM JavaScript footer for the current web transaction.
     */
    String getBrowserTimingFooter();

    /**
     * Set the user name to associate with the RUM JavaScript footer for the current web transaction.
     *
     * @param name User name to associate with the RUM JavaScript footer.
     */
    void setUserName(String name);

    /**
     * Set the account name to associate with the RUM JavaScript footer for the current web transaction.
     *
     * @param name Account name to associate with the RUM JavaScript footer.
     */
    void setAccountName(String name);

    /**
     * Set the product name to associate with the RUM JavaScript footer for the current web transaction.
     *
     * @param name Product name to associate with the RUM JavaScript footer.
     */
    void setProductName(String name);

    /**
     * Set the app server port which is reported to RPM.
     *
     * @param port the app server port
     */
    void setAppServerPort(int port);

    /**
     * Set the dispatcher name and version which is reported to RPM.
     *
     * @param dispatcherName the dispatcher name that is reported to RPM
     * @param version the version that is reported to RPM
     */
    void setServerInfo(String dispatcherName, String version);

    /**
     * Set the instance name in the environment. A single host:port may support multiple JVM instances. The instance
     * name is intended to help the customer identify the specific JVM instance.
     *
     * @param instanceName the instance name to set in the environment
     */
    void setInstanceName(String instanceName);

}
