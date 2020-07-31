/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

/**
 * A marker for tracers which do not want to participate in transaction trace. These tracers shouldn't be added to the
 * tracer stack for transactions.
 */
public interface SkipTracer extends Tracer {

}
