/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.core.type;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.micronaut.core.type.UnsafeExecutable", type = MatchType.Interface)
public abstract class UnsafeExecutable_Instrumentation<T, R> {

    public R invokeUnsafe(T instance, Object... arguments) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "UnsafeExecutable", getClass().getSimpleName(),"invoke");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Instance", instance.getClass().getName());
        return Weaver.callOriginal();
    }
}