/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface MessageBrokerConfig {
    /**
     * @return true if message_broker_tracer.instance_reporting.enabled is enabled
     */
    boolean isInstanceReportingEnabled();
}