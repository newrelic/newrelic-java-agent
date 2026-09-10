/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.service.Service;

import java.util.Map;

public interface ConfigService extends Service {

    void addIAgentConfigListener(AgentConfigListener listener);

    void removeIAgentConfigListener(AgentConfigListener listener);

    /**
     * Get the initial settings in the configuration file without any end-user sensitive information.
     */
    Map<String, Object> getSanitizedLocalSettings();

    /**
     * Get the default Agent configuration. This includes both local and server-side configuration.
     *
     * @return an immutable object representing the current state of the Agent configuration
     */
    AgentConfig getDefaultAgentConfig();

    /**
     * Get the local Agent configuration. This does not include server-side configuration.
     *
     * @return an immutable object representing the current state of the Agent configuration
     */
    AgentConfig getLocalAgentConfig();

    /**
     * Get the Agent configuration. This includes both local and server-side configuration.
     *
     * @return an immutable object representing the current state of the Agent configuration
     */
    AgentConfig getAgentConfig(String appName);

    /**
     * Get the transaction tracer configuration. This includes both local and server-side configuration.
     *
     * @return an immutable object representing the current state of the transaction tracer configuration
     */
    TransactionTracerConfig getTransactionTracerConfig(String appName);

    /**
     * Get the error collector configuration. This includes both local and server-side configuration.
     *
     * @return an immutable object representing the current state of the error collector configuration
     */
    ErrorCollectorConfig getErrorCollectorConfig(String appName);

    /**
     * Gets the exception stripping config.
     *
     * @param appName The specific application name.
     * @return The current state of the exception stripping configuration.
     */
    StripExceptionConfig getStripExceptionConfig(String appName);

    /**
     * Gets the distributed tracing config.
     *
     * @param appName The specific application name.
     * @return The current state of the distributed tracing configuration.
     */
    DistributedTracingConfig getDistributedTracingConfig(String appName);

    /**
     * Gets the extensions config.
     *
     * @param appName The specific application name.
     * @return The current state of the extensions configuration.
     */
    ExtensionsConfig getExtensionsConfig(String appName);

    /**
     * Sets the LASP policies received from the server side. The agent is responsible for converting these
     * polices into local agent configuration.
     */
    void setLaspPolicies(Map<String, Boolean> policiesJson);

    /**
     * Return a simple map of those config settings that were explicitly set via the yaml config file,
     * system properties or environment variables.
     *
     * @return a Map of explicitly set config settings
     */
    Map<String, Object> getExplicitlySetConfig();
}
