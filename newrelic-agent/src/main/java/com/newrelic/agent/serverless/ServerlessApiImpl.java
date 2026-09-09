/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.serverless;

import com.newrelic.agent.bridge.ServerlessApi;
import com.newrelic.agent.service.ServiceFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Implementation of ServerlessApi that stores serverless metadata for serverless mode.
 * This class acts as a bridge between serverless instrumentation and the core agent,
 * storing metadata that will be included in the serverless payload envelope.
 */
public class ServerlessApiImpl implements ServerlessApi {

    @Override
    public void setArn(String arnValue) {
        ServiceFactory.getServiceManager().getServerlessService().setArn(arnValue);
    }

    @Override
    public void setFunctionVersion(String version) {
        ServiceFactory.getServiceManager().getServerlessService().setFunctionVersion(version);
    }

    @Override
    public String getArn() {
        return ServiceFactory.getServiceManager().getServerlessService().getArn();
    }

    @Override
    public String getFunctionVersion() {
        return ServiceFactory.getServiceManager().getServerlessService().getFunctionVersion();
    }

    @Override
    public boolean isApmLambdaModeEnabled() {
        return ServiceFactory.getServiceManager().getServerlessService().isApmLambdaModeEnabled();
    }

    @Override
    public void removeMetricCollector(Object metricReader) {
        ServiceFactory.getServiceManager().getServerlessService().removeMetricCollector(metricReader);
    }

    @Override
    public void addMetricCollector(Object metricReader, Consumer<Object> metricCollector) {
        ServiceFactory.getServiceManager().getServerlessService().addMetricCollector(metricReader, metricCollector);
    }

    @Override
    public boolean otelHarvest(Supplier<String> metricPayloadProvider) {
        return ServiceFactory.getServiceManager().getServerlessService().otelHarvest(null);
    }
}
