/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import java.util.Map;

import javax.management.Attribute;
import javax.management.ObjectInstance;

import com.newrelic.agent.stats.StatsEngine;

/**
 * A custom jmx attribute processor.
 */
public interface JmxAttributeProcessor {

    /**
     * Returns true if this processor recorded stats for the given jmx attribute.
     */
    boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName,
            Map<String, Float> values);

}
