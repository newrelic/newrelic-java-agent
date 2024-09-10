/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Map;

import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.ExtensionsConfig;
import com.newrelic.agent.config.StripExceptionConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.service.AbstractService;

public class MockConfigService extends AbstractService implements ConfigService {

    private AgentConfig agentConfig2;

    public MockConfigService(AgentConfig agentConfig) {
        super(ConfigService.class.getSimpleName());
        agentConfig2 = agentConfig;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public AgentConfig getAgentConfig(String appName) {
        return agentConfig2;
    }

    public void setAgentConfig(AgentConfig agentConfig2) {
        this.agentConfig2 = agentConfig2;
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig(String appName) {
        return agentConfig2.getTransactionTracerConfig();
    }

    @Override
    public ErrorCollectorConfig getErrorCollectorConfig(String appName) {
        return null;
    }

    @Override
    public AgentConfig getDefaultAgentConfig() {
        return agentConfig2;
    }

    @Override
    public void addIAgentConfigListener(AgentConfigListener listener) {
    }

    @Override
    public void removeIAgentConfigListener(AgentConfigListener listener) {
    }

    @Override
    public AgentConfig getLocalAgentConfig() {
        return null;
    }

    @Override
    public Map<String, Object> getSanitizedLocalSettings() {
        return null;
    }

    @Override
    public StripExceptionConfig getStripExceptionConfig(String appName) {
        return null;
    }

    @Override
    public DistributedTracingConfig getDistributedTracingConfig(String appName) {
        return null;
    }

    @Override
    public ExtensionsConfig getExtensionsConfig(String appName) {
        return null;
    }

    @Override
    public void setLaspPolicies(Map<String, Boolean> policiesJson) {
    }

    @Override
    public Map<String, Object> getExplicitlySetConfig() {
        return null;
    }

}
