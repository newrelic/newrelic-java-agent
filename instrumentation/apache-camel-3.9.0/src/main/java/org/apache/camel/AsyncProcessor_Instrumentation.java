/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.camel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.camel.AsyncProcessor", type = MatchType.Interface)
public class AsyncProcessor_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (exchange instanceof Exchange_Instrumentation) {
            if (((Exchange_Instrumentation) exchange).token != null) {
                ((Exchange_Instrumentation) exchange).token.link();
            }
        }

        return Weaver.callOriginal();
    }
}
