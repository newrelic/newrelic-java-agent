/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.util.List;
import java.util.Map;

public interface TraceSegment {

    String getName();

    long getRelativeStartTime();

    long getRelativeEndTime();

    Map<String, Object> getTracerAttributes();

    List<TraceSegment> getChildren();

    String getClassName();

    String getMethodName();

    String getUri();

    int getCallCount();
}