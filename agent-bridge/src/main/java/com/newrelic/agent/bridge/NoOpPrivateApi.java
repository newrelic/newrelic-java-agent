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

public class NoOpPrivateApi implements PrivateApi {

    @Override
    public Closeable addSampler(Runnable sampler, int period, TimeUnit minutes) {
        return null;
    }

    @Override
    public void setServerInfo(String serverInfo) {
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
    }

    @Override
    public void addCustomAttribute(String key, String value) {
    }

    @Override
    public void addCustomAttribute(String key, Map<String, String> values) {
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
    }

    @Override
    public void addMBeanServer(MBeanServer server) {
    }

    @Override
    public void removeMBeanServer(MBeanServer serverToRemove) {
    }

    @Override
    public void addTracerParameter(String key, Number value) {
    }

    @Override
    public void reportHTTPError(String message, int statusCode, String uri) {
    }

    @Override
    public void reportException(Throwable throwable) {
    }

    @Override
    public void setAppServerPort(int port) {
    }

    @Override
    public void setServerInfo(String dispatcherName, String version) {
    }

    @Override
    public void setInstanceName(String instanceName) {
    }

    @Override
    public void addTracerParameter(String key, String value) {
    }

    @Override
    public void addTracerParameter(String key, String value, boolean addToSpan) {
    }

    @Override
    public void addTracerParameter(String key, Map<String, String> values) {
    }

}
