/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.ServerlessApi;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of ServerlessApi that stores serverless metadata for serverless mode.
 * This class acts as a bridge between serverless instrumentation and the core agent,
 * storing metadata that will be included in the serverless payload envelope.
 */
public class ServerlessApiImpl implements ServerlessApi {

    private final AtomicReference<String> arn = new AtomicReference<>();
    private final AtomicReference<String> functionVersion = new AtomicReference<>();

    @Override
    public void setArn(String arnValue) {
        if (arnValue != null && !arnValue.isEmpty()) {
            arn.set(arnValue);
        }
    }

    @Override
    public void setFunctionVersion(String version) {
        if (version != null && !version.isEmpty()) {
            functionVersion.set(version);
        }
    }

    @Override
    public String getArn() {
        return arn.get();
    }

    @Override
    public String getFunctionVersion() {
        return functionVersion.get();
    }
}
