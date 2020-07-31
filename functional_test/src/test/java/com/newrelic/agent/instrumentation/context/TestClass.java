/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestClass extends HttpURLConnection {

    protected TestClass(URL u) {
        super(u);
    }

    public static final String[] methodsToTrace = { "aMethodToTrace", "anotherMethodToTrace", "youGetTheIdea" };

    public void aMethodToTrace() throws InterruptedException {
        Thread.sleep(2);
    }

    public void anotherMethodToTrace() throws InterruptedException {
        Thread.sleep(2);
    }

    public void youGetTheIdea() throws InterruptedException {
        Thread.sleep(2);
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    // throwing a pointcut into the mix
    @Override
    public void connect() throws IOException {
    }
}
