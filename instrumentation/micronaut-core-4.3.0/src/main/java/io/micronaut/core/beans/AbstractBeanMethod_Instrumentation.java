/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.core.beans;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.micronaut.core.beans.AbstractBeanMethod", type = MatchType.BaseClass)
public abstract class AbstractBeanMethod_Instrumentation<B, T> {

    public abstract String getName();

    protected T invokeInternal(B instance, Object... arguments) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "AbstractBeanMethod", getClass().getSimpleName(), "invoke");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Instance", instance.getClass().getName());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Name", getName());
        return Weaver.callOriginal();
    }
}