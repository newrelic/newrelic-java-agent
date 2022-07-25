/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Map;

/**
 * The New Relic API. Consumers of this API should add the newrelic-api.jar to their classpath. The static methods of
 * this class form the Agent's basic Java API. Use {@link NewRelic#getAgent} to obtain the root of a hierarchy of
 * objects offering additional capabilities.
 */
public final class NewRelic {

    /**
     * Returns the root of the New Relic Java Agent API object hierarchy.
     *
     * @return the root of the New Relic Java Agent API object hierarchy
     * @since 3.9.0
     */
    public static Agent getAgent() {
        return NoOpAgent.INSTANCE;
    }

    // ************************** Metric API ****************************************//

    /**
     * Record a metric value for the given name.
     *
     * @param name The name of the metric. The metric is not recorded if the name is null or the empty string.
     * @param value The value of the metric.
     * @since 1.3.0
     */
    public static void recordMetric(String name, float value) {
    }

    /**
     * Record a response time in milliseconds for the given metric name.
     *
     * @param name The name of the metric. The response time is not recorded if the name is null or the empty string.
     * @param millis The response time in milliseconds.
     * @since 1.3.0
     */
    public static void recordResponseTimeMetric(String name, long millis) {
    }

    /**
     * Increment the metric counter for the given name.
     *
     * @param name The name of the metric to increment.
     * @since 1.3.0
     */
    public static void incrementCounter(String name) {
    }

    /**
     * Increment the metric counter for the given name.
     *
     * @param name The name of the metric to increment.
     * @param count The amount in which the metric should be incremented.
     * @since 2.21.0
     */
    public static void incrementCounter(String name, int count) {
    }

    // ************************** Error API ***********************************//

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param throwable The throwable to notice and report.
     * @param params Custom parameters to include in the traced error. May be null.
     * @since 1.3.0
     */
    public static void noticeError(Throwable throwable, Map<String, ?> params) {
    }

    /**
     * Report an exception to New Relic.
     *
     * @param throwable The throwable to report.
     * @see #noticeError(Throwable, Map)
     * @since 1.3.0
     */
    public static void noticeError(Throwable throwable) {
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param message The error message to be reported.
     * @param params Custom parameters to include in the traced error. May be null.
     * @since 1.3.0
     */
    public static void noticeError(String message, Map<String, ?> params) {
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * @param message Message to report with a transaction when it finishes.
     * @since 2.21.0
     */
    public static void noticeError(String message) {
    }

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
     * @since 3.38.0
     */
    public static void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
    }

    /**
     * Report an exception to New Relic.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param throwable The throwable to report.
     * @param expected true if this error is expected, false otherwise.
     * @see #noticeError(Throwable, Map)
     * @since 3.38.0
     */
    public static void noticeError(Throwable throwable, boolean expected) {
    }

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
     * @since 3.38.0
     */
    public static void noticeError(String message, Map<String, ?> params, boolean expected) {
    }

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
     * @since 3.38.0
     */
    public static void noticeError(String message, boolean expected) {
    }

    // **************************** Transaction APIs ********************************//

    /**
     * Add a key/value pair to the current transaction. These are reported in errors, transaction traces, and
     * transaction events. The key and value will only be reported if this call is made within a New Relic transaction.
     * <p>
     * <b>Note:</b> The key and value pair will only be reported if the key argument can be represented in 255 bytes
     * when encoded with UTF-8 encoding.
     * </p>
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     * @since 1.3.0
     */
    public static void addCustomParameter(String key, Number value) {
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors, transaction traces, and
     * transaction events. The key and value will only be reported if this call is made within a New Relic transaction.
     * <p>
     * <b>Note:</b> The key and value pair will only be reported if the key argument can be represented in 255 bytes
     * when encoded with UTF-8 encoding. The value argument will be truncated, by stripping characters, to fit in 255
     * bytes, encoded using UTF-8 encoding.
     * </p>
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     * @since 1.3.0
     */
    public static void addCustomParameter(String key, String value) {
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors, transaction traces, and
     * transaction events. The key and value will only be reported if this call is made within a New Relic transaction.
     * <p>
     * <b>Note:</b> The key and value pair will only be reported if the key argument can be represented in 255 bytes
     * when encoded with UTF-8 encoding.
     * </p>
     *
     * @param key Custom parameter key.
     * @param value Custom parameter value.
     * @since 6.1.0
     */
    public static void addCustomParameter(String key, boolean value) {
    }

    /**
     * Add key/value pairs to the current transaction. These are reported in errors, transaction traces, and
     * transaction events. The key and value will only be reported if this call is made within a New Relic transaction.
     * <p>
     * <b>Note:</b> The key and value pairs will only be reported if the key argument can be represented in 255 bytes
     * when encoded with UTF-8 encoding. The value argument will be truncated, by stripping characters, to fit in 255
     * bytes, encoded using UTF-8 encoding.
     * </p>
     *
     * @param params Custom parameters to include.
     * @since 4.12.0
     */
    public static void addCustomParameters(Map<String, Object> params) {
    }

    /**
     * Set the name of the current transaction.
     *
     * @param category Metric category. If the input is null, then the default will be used.
     * @param name The name of the transaction starting with a forward slash. example: /store/order
     * @since 1.3.0
     */
    public static void setTransactionName(String category, String name) {
    }

    /**
     * Ignore the current transaction.
     *
     * @since 1.3.0
     */
    public static void ignoreTransaction() {
    }

    /**
     * Ignore the current transaction for calculating Apdex score.
     *
     * @since 1.3.0
     */
    public static void ignoreApdex() {
    }

    /**
     * Sets the request and response instances for the current transaction. Use this API to generate web transactions
     * for custom web request dispatchers. Only the first call to setRequestAndResponse will take effect.
     *
     * @param request The current transaction's request.
     * @param response The current transaction's response.
     * @since 2.21.0
     */
    public static void setRequestAndResponse(Request request, Response response) {
    }

    // **************************** Real User Monitoring (RUM) ********************************
    // API calls to support manual instrumentation of RUM if auto instrumentation is not available.
    // Get the JavaScript header and footer that should be inserted into the HTML response for browser-side monitoring.

    /**
     * Get the RUM JavaScript header for the current web transaction.
     *
     * @return RUM JavaScript header for the current web transaction.
     * @since 2.21.0
     */
    public static String getBrowserTimingHeader() {
        return "";
    }

    /**
     * Get the RUM JavaScript header for the current web transaction.
     * @param nonce a random per-request nonce for sites using Content Security Policy (CSP)
     * @return RUM JavaScript header for the current web transaction.
     * @since 7.6.0
     */
    public static String getBrowserTimingHeader(String nonce) {
        return "";
    }

    /**
     * Get the RUM JavaScript footer for the current web transaction.
     *
     * @return RUM JavaScript footer for the current web transaction.
     * @since 2.21.0
     */
    public static String getBrowserTimingFooter() {
        return "";
    }

    /**
     * Get the RUM JavaScript footer for the current web transaction.
     * @param nonce a random per-request nonce for sites using Content Security Policy (CSP)
     * @return RUM JavaScript footer for the current web transaction.
     * @since 7.6.0
     */
    public static String getBrowserTimingFooter(String nonce) {
        return "";
    }

    /**
     * Set the user name to associate with the RUM JavaScript footer for the current web transaction.
     *
     * <p>
     * <b>Note:</b> The user {@code name} argument has a limit of 255 bytes, encoded with UTF-8 encoding. A {@code name}
     * that requires more than 255 bytes will be truncated, by stripping characters, to fit in 255 bytes.
     * </p>
     *
     * @param name User name to associate with the RUM JavaScript footer.
     * @since 2.21.0
     */
    public static void setUserName(String name) {
    }

    /**
     * Set the account name to associate with the RUM JavaScript footer for the current web transaction.
     *
     * <p>
     * <b>Note:</b> The account {@code name} argument has a limit of 255 bytes, encoded with UTF-8 encoding. A
     * {@code name} that requires more than 255 bytes will be truncated, by stripping characters, to fit in 255 bytes.
     * </p>
     *
     * @param name Account name to associate with the RUM JavaScript footer.
     * @since 2.21.0
     */
    public static void setAccountName(String name) {
    }

    /**
     * Set the product name to associate with the RUM JavaScript footer for the current web transaction.
     *
     * <p>
     * <b>Note:</b> The product {@code name} argument has a limit of 255 bytes, encoded with UTF-8 encoding. A
     * {@code name} that requires more than 255 bytes will be truncated, by stripping characters, to fit in 255 bytes.
     * </p>
     *
     * @param name Product name to associate with the RUM JavaScript footer.
     * @since 2.21.0
     */
    public static void setProductName(String name) {
    }

    // **************************** Web Frameworks API ********************************

    /**
     * Set the app server port which is reported to RPM.
     *
     * @param port the app server port
     * @since 3.36.0
     */
    public static void setAppServerPort(int port) {
    }

    /**
     * Set the dispatcher name and version which is reported to RPM.
     *
     * @param dispatcherName the dispatcher name that is reported to RPM
     * @param version the version that is reported to RPM
     * @since 3.36.0
     */
    public static void setServerInfo(String dispatcherName, String version) {
    }

    /**
     * Set the instance name in the environment. A single host:port may support multiple JVM instances. The instance
     * name is intended to help the customer identify the specific JVM instance.
     *
     * @param instanceName the instance name to set in the environment
     * @since 3.36.0
     */
    public static void setInstanceName(String instanceName) {
    }

}
