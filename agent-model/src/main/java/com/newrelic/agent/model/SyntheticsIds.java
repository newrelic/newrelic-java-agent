/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public class SyntheticsIds {

    private final String resourceId;
    private final String monitorId;
    private final String jobId;

    public SyntheticsIds(String resourceId, String monitorId, String jobId) {
        this.resourceId = resourceId;
        this.monitorId = monitorId;
        this.jobId = jobId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getMonitorId() {
        return monitorId;
    }

    public String getJobId() {
        return jobId;
    }
}
