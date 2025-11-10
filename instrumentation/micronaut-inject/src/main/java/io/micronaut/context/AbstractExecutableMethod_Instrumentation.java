/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.context;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.micronaut.context.AbstractExecutableMethod", type = MatchType.BaseClass)
public abstract class AbstractExecutableMethod_Instrumentation {
    public abstract String getMethodName();
    public abstract Class getDeclaringType();

    @Trace
    public Object invoke(Object instance, Object... arguments) {
        Class<?> dType = getDeclaringType();
        String classname = "Unknown";
        if(dType != null) {
            String tmp = dType.getSimpleName();
            if(tmp != null && !tmp.isEmpty()) {
                classname = tmp;
            }
        }
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","ExecutableMethod",getClass().getSimpleName(),"invoke",classname,getMethodName());
        return Weaver.callOriginal();
    }
}