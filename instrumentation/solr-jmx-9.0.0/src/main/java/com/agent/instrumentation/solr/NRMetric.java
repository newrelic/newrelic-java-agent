/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

public abstract class NRMetric {

    protected static final String prefix = "JMX/solr/";

    public NRMetric(String r, String b) {
        registry = r;
        name = b;
    }

    protected String registry;

    protected String name;

    protected String contextTag;

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getContextTag() {
        return contextTag;
    }

    public void setContextTag(String contextTag) {
        this.contextTag = contextTag;
    }

    public abstract String getMetricName(String name);

    public abstract int reportMetrics();

    public abstract String getMetricBase();

}
