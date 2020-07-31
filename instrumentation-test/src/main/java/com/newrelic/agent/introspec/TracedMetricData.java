/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

/**
 * Currently the Java agent times TracedMethods and TracedActivities. This is the metric data associated with each
 * traced method/activity.
 */
public interface TracedMetricData {

    String getName();

    int getCallCount();

    float getTotalTimeInSec();

    float getExclusiveTimeInSec();

}