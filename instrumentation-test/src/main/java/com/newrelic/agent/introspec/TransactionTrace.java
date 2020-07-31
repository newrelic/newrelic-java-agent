/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.util.Map;

public interface TransactionTrace {

    long getStartTime();

    float getResponseTimeInSec();

    float getWallClockDurationInSec();

    TraceSegment getInitialTraceSegment();

    Map<String, Object> getIntrinsicAttributes();

}
