/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

/**
 * Determines how the metric should be reported.
 */
public enum JMXMetricType {

    /**
     * This is the default. It creates a new metric for each mbean. If three mbeans match the mbean query, then each one
     * will be recorded as a metric (end stats count will be 3).
     */
    INCREMENT_COUNT_PER_BEAN,
    /**
     * This will sum all beans matching the query together with a count of 1. This feature will be ignored for J2EE
     * objects and Websphere objects. It can only be used for general number attribute values.
     */
    SUM_ALL_BEANS;

}
