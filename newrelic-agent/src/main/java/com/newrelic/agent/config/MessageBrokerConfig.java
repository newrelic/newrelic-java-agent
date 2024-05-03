package com.newrelic.agent.config;

public interface MessageBrokerConfig {
    /**
     * @return true if message_broker_tracer.instance_reporting.enabled is enabled
     */
    boolean isInstanceReportingEnabled();
}
