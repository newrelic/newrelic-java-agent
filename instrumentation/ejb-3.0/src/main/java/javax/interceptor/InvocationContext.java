/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.interceptor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class InvocationContext {

    @NewField
    private static final String EJB = "EJB";

    public abstract Object getTarget();

    @Trace(dispatcher = true)
    public Object proceed() throws Exception {
        // Name the transaction using the EJB name.
        Object target = getTarget();
        if (target != null) {
            String targetName = target.getClass().getName();
            NewRelic.getAgent().getTracedMethod().setMetricName(
                    "EJB", targetName);
        }
        return Weaver.callOriginal();
    }
}
