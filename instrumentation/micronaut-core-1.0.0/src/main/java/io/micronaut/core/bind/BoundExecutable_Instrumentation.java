/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.core.bind;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.micronaut.core.bind.BoundExecutable", type = MatchType.Interface)
public abstract class BoundExecutable_Instrumentation<T, R> {

    @Trace
    public R invoke(T instance) {
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        traced.setMetricName("Micronaut", "BoundExecutable", getClass().getSimpleName(), "invoke");
        traced.addCustomAttribute("Instance", instance.getClass().getName());
        return Weaver.callOriginal();
    }
}