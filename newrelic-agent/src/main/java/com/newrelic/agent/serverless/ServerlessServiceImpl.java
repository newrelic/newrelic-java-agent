/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.serverless;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class ServerlessServiceImpl extends AbstractService implements ServerlessService {
    private final AtomicReference<String> arn = new AtomicReference<>();
    private final AtomicReference<String> functionVersion = new AtomicReference<>();

    private final ConcurrentHashMap<Object, Consumer<Object>> openTelemetryMetricCollectors = new ConcurrentHashMap<>();

    private final AtomicReference<String> otelMetricPayload = new AtomicReference<>();

    private final Object harvestLock = new Object();

    public ServerlessServiceImpl() {
        super(ServerlessService.class.getSimpleName());
    }

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

    @Override
    public boolean isApmLambdaModeEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().isApmLambdaModeEnabled();
    }

    @Override
    public boolean otelMetricsRegistered() {
        return !openTelemetryMetricCollectors.isEmpty();
    }

    @Override
    public String otelMetricsPayload() {
        return otelMetricPayload.get();
    }

    @Override
    public void addMetricCollector(Object metricReader, Consumer<Object> metricCollector) {
        if (metricReader == null || metricCollector == null) {
            return;
        }
        openTelemetryMetricCollectors.put(metricReader, metricCollector);
    }

    @Override
    public void removeMetricCollector(Object metricReader) {
        openTelemetryMetricCollectors.remove(metricReader);
    }

    @Override
    public void collectOtelMetrics() {
        for (Map.Entry<Object, Consumer<Object>> entry : openTelemetryMetricCollectors.entrySet()) {
            Object metricReader = entry.getKey();
            Consumer<Object> metricCollector = entry.getValue();
            metricCollector.accept(metricReader);
        }
    }


    @Override
    public boolean otelHarvest(String metricPayload) {
        try {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().getServerlessConfig().isEnabled()) {
                synchronized (harvestLock) {
                    otelMetricPayload.set(metricPayload);
                    ServiceFactory.getServiceManager().getHarvestService().harvestNow();
                }
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Failed to harvest metrics for serverless Open Telemetry", e);
            return false;
        }

        return true;
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public boolean isEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getServerlessConfig().isEnabled();
    }
}
