/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import java.util.Map;

/**
 * This class is thread safe.
 */
public class MockBrowserTransactionState implements BrowserTransactionState {

    private volatile String transactionName;
    private volatile long externalTime;
    private volatile long duration;
    private volatile String appName;

    @Override
    public long getDurationInMilliseconds() {
        return duration;
    }

    public void setDurationInMilliseconds(long duration) {
        this.duration = duration;
    }

    @Override
    public long getExternalTimeInMilliseconds() {
        return externalTime;
    }

    public void setExternalTimeInMilliseconds(long externalTime) {
        this.externalTime = externalTime;
    }

    @Override
    public String getBrowserTimingHeader() {
        return null;
    }

    @Override
    public String getBrowserTimingHeader(String nonce) {
        return null;
    }

    @Override
    public String getBrowserTimingHeaderForJsp() {
        return null;
    }

    @Override
    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return null;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return null;
    }

    public void setAppName(String name) {
        appName = name;
    }

    @Override
    public String getAppName() {
        return appName;
    }
}
