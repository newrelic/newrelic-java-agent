/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */


package slick.util;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import scala.Function0;
import scala.Function1;
import slick.util.AsyncExecutor.PrioritizedRunnable;

@Weave(type = MatchType.Interface, originalName = "slick.util.AsyncExecutor")
public class AsyncExecutor_Instrumentation {
    public PrioritizedRunnable prioritizedRunnable(Function0 priority, Function1 run) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        Token token = null;
        if ( txn != null) {
            token = txn.getToken();
            run = AsyncExecutorUtil.wrapRunMethod(run, token);
        }
        return Weaver.callOriginal();
    }
}
