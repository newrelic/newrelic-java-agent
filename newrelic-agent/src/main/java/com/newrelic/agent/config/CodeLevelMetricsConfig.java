package com.newrelic.agent.config;

/**
 * Settings for the Code Level Metrics functionality.
 * @since 7.10
 */
public interface CodeLevelMetricsConfig {

    /**
     * Determines whether CLM functionality should be enabled.
     * @return true if the functionality should be enabled, false otherwise
     * @since 7.10
     */
    boolean isEnabled();
}
