/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Provides access to agent configuration settings. The key names are flattened representations of values in the
 * configuration file.
 * 
 * Example keys: <br>
 * transaction_tracer.enabled <br>
 * instrumentation.hibernate.stat_sampler.enabled
 */
public interface Config {

    /**
     * Get the value of a setting.
     * 
     * @param <T> This is the type parameter
     * @param key The flattened configuration setting key.
     * @return The value of the property or null if the value is absent.
     * @since 3.9.0
     */
    <T> T getValue(String key);

    /**
     * Get the value of a setting, returning the default if the value is not present.
     * 
     * @param <T> This is the type parameter
     * @param key The flattened configuration setting key.
     * @param defaultVal The default value to return if the given key is not present.
     * @return The value of the property or defaultVal if the value is absent.
     * @since 3.9.0
     */
    <T> T getValue(String key, T defaultVal);
}
