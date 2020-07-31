/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.work;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleUtils;

import javax.resource.spi.work.Work;

@Weave(type = MatchType.ExactClass, originalName = "org.mule.work.WorkerContext")
public abstract class WorkerContext_Instrumentation {

    @NewField
    private Token asyncToken;

    private Work worker = Weaver.callOriginal();

    @Trace(async = true, excludeFromTransactionTrace = true)
    public synchronized void workAccepted(Object anObject) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            String className = worker.getClass().getName();
            if (MuleUtils.ignoreClass(className)) {
                txn.ignore();
            } else {
                asyncToken = txn.getToken();
            }
        }

        Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void run() {
        if (asyncToken != null) {
            asyncToken.linkAndExpire();
            asyncToken = null;
        }

        Weaver.callOriginal();
    }

}
