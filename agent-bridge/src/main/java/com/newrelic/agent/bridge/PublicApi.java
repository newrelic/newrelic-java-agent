/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.ErrorApi;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

import java.util.Map;

/**
 * The public api interface.
 */
public interface PublicApi extends ErrorApi {

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
     * Sets the user ID for the current transaction by adding the "enduser.id" agent attribute. It is reported in errors and transaction traces.
     * When high security mode is enabled, this method call will do nothing.
     *
     * @param userId The user ID to report. If it is a null or blank String, the "enduser.id" agent attribute will not be included in the current transaction and any associated errors.
     */
    void setUserId(String userId);

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
     * Get the RUM JavaScript header for the current web transaction.
     * @param nonce a random per-request nonce for sites using Content Security Policy (CSP)
     * @return RUM JavaScript header for the current web transaction.
     */
    String getBrowserTimingHeader(String nonce);

    /**
     * Set the user name for the current web transaction.
     * If high security mode is enabled, this method call does nothing.
     *
     * @param name User name for the current web transaction.
     */
    void setUserName(String name);

    /**
     * Set the account name for the current web transaction.
     *
     * @param name Account name for the current web transaction.
     */
    void setAccountName(String name);

    /**
     * Set the product name for the current web transaction.
     *
     * @param name Product name for the current web transaction.
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
