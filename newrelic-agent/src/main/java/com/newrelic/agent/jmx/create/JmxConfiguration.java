/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import java.util.List;
import java.util.Map;

import com.newrelic.agent.jmx.JmxType;

public interface JmxConfiguration {

    /**
     * Gets the object name from the JMX configuration.
     * 
     * @return The object name as a string.
     */
    String getObjectName();

    /**
     * Returns the root metric name (the starting path of the metric name), or null if none was specified.
     * It's possible to pull keys out of the object name using {key_name}.  It's also possible to
     * use attribute values by referring to the attribute using {:attr_name:}.
     */
    String getRootMetricName();

    /**
     * Gets the enabled flag from the JMX configuration.
     * 
     * @return True if the property is not present of is set to true, else false.
     */
    boolean getEnabled();

    /**
     * Parses the JMX configuration and returns a map of JMX type to attributes.
     * 
     * @return Map of JMX types to attributes listed in the JMX configuration.
     */
    Map<JmxType, List<String>> getAttrs();
}
