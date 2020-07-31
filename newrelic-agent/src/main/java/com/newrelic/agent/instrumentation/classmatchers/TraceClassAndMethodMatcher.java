/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;

public interface TraceClassAndMethodMatcher extends ClassAndMethodMatcher {

    TraceDetails getTraceDetails();

}
