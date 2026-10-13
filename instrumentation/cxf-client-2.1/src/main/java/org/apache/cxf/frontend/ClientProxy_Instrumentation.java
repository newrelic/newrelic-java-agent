/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.cxf.frontend;

import java.lang.reflect.Method;

import org.apache.cxf.service.model.BindingOperationInfo;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.cxf.frontend.ClientProxy", type = MatchType.ExactClass)
public class ClientProxy_Instrumentation {

    @Trace
    public Object invokeSync(Method method, BindingOperationInfo oi, Object[] params) throws Exception {
        NewRelic.getAgent().getTracedMethod().setMetricName("Java", method.getDeclaringClass().getName(),
                method.getName());
        return Weaver.callOriginal();
    }
}
