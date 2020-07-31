/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

abstract class RequestImpl {

    private String originalMetricName;
    private String host;
    private int count;

    protected RequestImpl(String metricName, String host) {
        this.originalMetricName = metricName;
        this.host = host;
        this.count = 1;
    }

    protected boolean wasMerged(RequestImpl potential) {
        if (originalMetricName != null && originalMetricName.equals(potential.originalMetricName)) {
            count++;
            return true;
        }
        return false;
    }

    public String getMetricName() {
        return originalMetricName;
    }

    public String getHostname() {
        return host;
    }

    public int getCount() {
        return count;
    }
}
