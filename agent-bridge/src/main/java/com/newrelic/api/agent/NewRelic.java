/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Map;

import com.newrelic.agent.bridge.AgentBridge;

/**
 * The New Relic API that is used at runtime. All method invocations call through to {@link AgentBridge#publicApi}.
 */
public final class NewRelic {

    public static Agent getAgent() {
        return AgentBridge.agent;
    }

    // ************************** Metric API ****************************************//

    /**
     * Record a metric value for the given name.
     * 
     * @param name The name of the metric. The metric is not recorded if the name is null or the empty string.
     * @param value The value of the metric.
     */
    public static void recordMetric(String name, float value) {
        getAgent().getMetricAggregator().recordMetric(name, value);
    }

    /**
     * Record a response time in milliseconds for the given metric name.
     * 
     * @param name The name of the metric. The response time is not recorded if the name is null or the empty string.
     * @param millis The response time in milliseconds.
     */
    public static void recordResponseTimeMetric(String name, long millis) {
        getAgent().getMetricAggregator().recordResponseTimeMetric(name, millis);
    }

    /**
     * Increment the metric counter for the given name.
     * 
     * @param name The name of the metric to increment.
     */
    public static void incrementCounter(String name) {
        getAgent().getMetricAggregator().incrementCounter(name);
    }

    /**
     * Increment the metric counter for the given name.
     * 
     * @param name The name of the metric to increment.
     * @param count The amount in which the metric should be incremented.
     */
    public static void incrementCounter(String name, int count) {
        getAgent().getMetricAggregator().incrementCounter(name, count);
    }

    // ************************** Error collector ***********************************//

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     * 
     * @param throwable The throwable to notice and report.
     * @param params Custom parameters to include in the traced error. May be null.
     */
    public static void noticeError(Throwable throwable, Map<String, ?> params) {
        AgentBridge.publicApi.noticeError(throwable, params);
    }

    /**
     * Report an exception to New Relic.
     * 
     * @param throwable The throwable to report.
     * @see #noticeError(Throwable, Map)
     */
    public static void noticeError(Throwable throwable) {
        AgentBridge.publicApi.noticeError(throwable);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     * 
     * @param message The error message to be reported.
     * @param params Custom parameters to include in the traced error. May be null
     */
    public static void noticeError(String message, Map<String, ?> params) {
        AgentBridge.publicApi.noticeError(message, params);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     * 
     * @param message Message to report with a transaction when it finishes.
     */
    public static void noticeError(String message) {
        AgentBridge.publicApi.noticeError(message);
    }

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
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
    public static void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        AgentBridge.publicApi.noticeError(throwable, params, expected);
    }

    /**
     * Report an exception to New Relic.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param throwable The throwable to report.
     * @param expected true if this error is expected, false otherwise.
     * @see #noticeError(Throwable, Map)
     */
    public static void noticeError(Throwable throwable, boolean expected) {
        AgentBridge.publicApi.noticeError(throwable, expected);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
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
     * @param params Custom parameters to include in the traced error. May be null
     * @param expected true if this error is expected, false otherwise.
     */
    public static void noticeError(String message, Map<String, ?> params, boolean expected) {
        AgentBridge.publicApi.noticeError(message, params, expected);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param message Message to report with a transaction when it finishes.
     * @param expected true if this error is expected, false otherwise.
     */
    public static void noticeError(String message, boolean expected) {
        AgentBridge.publicApi.noticeError(message, expected);
    }

    // **************************** Transaction APIs ********************************//

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     * 
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     */
    public static void addCustomParameter(String key, Number value) {
        AgentBridge.publicApi.addCustomParameter(key, value);
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     * 
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     */
    public static void addCustomParameter(String key, String value) {
        AgentBridge.publicApi.addCustomParameter(key, value);
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     */
    public static void addCustomParameter(String key, boolean value) {
        AgentBridge.publicApi.addCustomParameter(key, value);
    }

    /**
     * Add a key/value pairs to the current transaction. These are reported in errors and transaction traces.
     *
     * @param params Custom parameters to include.
     */
    public static void addCustomParameters(Map<String, Object> params) {
        AgentBridge.publicApi.addCustomParameters(params);
    }


    /**
     * Sets the user ID for the current transaction by adding the "enduser.id" agent attribute. It is reported in errors and transaction traces.
     * When high security mode is enabled, this method call will do nothing.
     *
     * @param userId The user ID to report. If it is a null or blank String, the "enduser.id" agent attribute will not be included in the current transaction and any associated errors.
     */
    public static void setUserId(String userId) {
        AgentBridge.publicApi.setUserId(userId);
    }

    /**
     * Set the name of the current transaction.
     * 
     * @param category Metric category. If the input is null, then the default will be used.
     * @param name The name of the transaction starting with a forward slash. example: /store/order
     */
    public static void setTransactionName(String category, String name) {
        AgentBridge.publicApi.setTransactionName(category, name);
    }

    /**
     * Ignore the current transaction.
     */
    public static void ignoreTransaction() {
        AgentBridge.publicApi.ignoreTransaction();
    }

    /**
     * Ignore the current transaction for calculating Apdex score.
     */
    public static void ignoreApdex() {
        AgentBridge.publicApi.ignoreApdex();
    }

    /**
     * Sets the request and response instances for the current transaction. Use this API to generate web transactions
     * for custom web request dispatchers. Only the first call to setRequestAndResponse will take effect.
     * 
     * @param request The current transaction's request.
     * @param response The current transaction's response.
     */
    public static void setRequestAndResponse(Request request, Response response) {
        AgentBridge.publicApi.setRequestAndResponse(request, response);
    }

    // **************************** Real User Monitoring (RUM) ********************************
    // API calls to support manual instrumentation of RUM if auto instrumentation is not available.
    // Get the JavaScript header and footer that should be inserted into the HTML response for browser-side monitoring.

    /**
     * Get the RUM JavaScript header for the current web transaction.
     * 
     * @return RUM JavaScript header for the current web transaction.
     */
    public static String getBrowserTimingHeader() {
        return AgentBridge.publicApi.getBrowserTimingHeader();
    }

    /**
     * Get the RUM JavaScript header for the current web transaction.
     * @param nonce a random per-request nonce for sites using Content Security Policy (CSP)
     * @return RUM JavaScript header for the current web transaction.
     */
    public static String getBrowserTimingHeader(String nonce) {
        return AgentBridge.publicApi.getBrowserTimingHeader(nonce);
    }

    /**
     * Set the user name for the current web transaction.
     * If high security mode is enabled, this method call does nothing.
     *
     * @param name User name for the current web transaction.
     */
    public static void setUserName(String name) {
        AgentBridge.publicApi.setUserName(name);
    }

    /**
     * Set the account name for the current web transaction.
     *
     * @param name Account name for the current web transaction.
     */
    public static void setAccountName(String name) {
        AgentBridge.publicApi.setAccountName(name);
    }

    /**
     * Set the product name for the current web transaction.
     *
     * @param name Product name for the current web transaction.
     */
    public static void setProductName(String name) {
        AgentBridge.publicApi.setProductName(name);
    }

    // **************************** Web Frameworks API ********************************

    /**
     * Set the app server port which is reported to RPM.
     *
     * @param port
     */
    public static void setAppServerPort(int port) {
        AgentBridge.publicApi.setAppServerPort(port);
    }

    /**
     * Set the dispatcher name and version which is reported to RPM.
     *
     * @param version
     */
    public static void setServerInfo(String dispatcherName, String version) {
        AgentBridge.publicApi.setServerInfo(dispatcherName, version);
    }

    /**
     * Set the instance name in the environment. A single host:port may support multiple JVM instances. The instance
     * name is intended to help the customer identify the specific JVM instance.
     *
     * @param instanceName
     */
    public static void setInstanceName(String instanceName) {
        AgentBridge.publicApi.setInstanceName(instanceName);
    }

    /**
     * Registers an {@link ErrorGroupCallback} that's used to generate a grouping key for the supplied
     * error. This key will be used to group similar error messages on the Errors Inbox UI. If the
     * errorGroupCallback instance is null no grouping key will be generated.
     *
     * @param errorGroupCallback the ErrorGroupCallback used to generate grouping keys for errors
     */
    public static void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        AgentBridge.publicApi.setErrorGroupCallback(errorGroupCallback);
    }

}
