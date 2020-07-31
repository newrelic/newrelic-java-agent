/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import org.objectweb.asm.commons.Method;

/**
 * A thing that tracks the {@link TraceDetails} for different {@link Method}s.
 */
public interface TraceDetailsList {

    void addTrace(Method method, TraceDetails traceDetails);

}
