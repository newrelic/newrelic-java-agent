/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import java.util.List;

public abstract class JmxFrameworkValues {

    /**
     * Creates this FrameworkJmxMetrics.
     */
    public JmxFrameworkValues() {
        super();
    }

    /**
     * The metrics to add for the framework.
     *
     * @return Metrics to be added to the JMXService.
     */
    public abstract List<BaseJmxValue> getFrameworkMetrics();

    /**
     * The jmx invokes which need to occur to get metrics.Generally this returns null. Override this method if you need
     * to invoke a method.
     *
     * @return Invokes which need to occur on the mbean servers.
     */
    public List<BaseJmxInvokeValue> getJmxInvokers() {
        return null;
    }

    /**
     * The prefix for all of the metrics.
     *
     * @return The prefix found in the object name.
     */
    public abstract String getPrefix();

}
