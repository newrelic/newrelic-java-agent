/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import javax.management.MBeanServer;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface PrivateApi {

    /**
     * Adds a sampler to be called at a given frequency.
     *
     * @param sampler
     * @param period
     * @param timeUnit
     * @return a Closeable which when executed will cancel the sampler execution.
     */
    Closeable addSampler(Runnable sampler, int period, TimeUnit timeUnit);

    void setServerInfo(String serverInfo);

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces. This will
     * add the parameter even if high_security is set. Use with caution.
     *
     * @param key   Custom parameter key.
     * @param value Custom parameter value. @
     */
    void addCustomAttribute(String key, Number value);

    /**
     * Add a key with a map of values to the current transaction. These are reported in errors and transaction traces.
     * This will add the parameter even if high_security is set. Use with caution.
     *
     * @param key    Custom parameter key.
     * @param values Custom parameter values. @
     */
    void addCustomAttribute(String key, Map<String, String> values);

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces. This will
     * add the parameter even if high_security is set. Use with caution.
     *
     * @param key   Custom parameter key.
     * @param value Custom parameter value. @
     */
    void addCustomAttribute(String key, String value);

    void addTracerParameter(String key, Number value);

    void addTracerParameter(String key, String value);

    void addTracerParameter(String key, String value, boolean addToSpan);

    void addTracerParameter(String key, Map<String, String> values);

    /**
     * Call if you want to pull JMX metrics from this server instead of the default MBeanServers.
     *
     * @param server The server to use for pulling MBeans.
     */
    void addMBeanServer(MBeanServer server);

    /**
     * Call if you want to remove a particular MBeanServer.
     *
     * @param serverToRemove The desired server to remove from the JMXService.
     */
    void removeMBeanServer(MBeanServer serverToRemove);

    /**
     * Report an HTTP error
     *
     * @param message    Error message
     * @param statusCode HTTP status code
     * @param uri        Request URI
     */
    void reportHTTPError(String message, int statusCode, String uri);

    /**
     * Report an exception. Checks if throwable should be ignored before reporting it.
     *
     * @param throwable
     */
    void reportException(Throwable throwable);

    /**
     * Set the app server port which is reported to RPM.
     *
     * @deprecated
     * Use {@link PublicApi#setAppServerPort(int)}
     *
     * @param port
     */
    @Deprecated
    void setAppServerPort(int port);

    /**
     * Set the dispatcher name and version which is reported to RPM.
     *
     * @deprecated
     * Use {@link PublicApi#setServerInfo(String, String)}
     *
     * @param version
     */
    @Deprecated
    void setServerInfo(String dispatcherName, String version);

    /**
     * Set the instance name in the environment. A single host:port may support multiple JVM instances. The instance
     * name is intended to help the customer identify the specific JVM instance.
     *
     * @deprecated
     * Use {@link PublicApi#setInstanceName(String)}
     *
     * @param instanceName
     */
    @Deprecated
    void setInstanceName(String instanceName);
}
