/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.core.propagation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.NRTokenCallableWrapper;
import com.nr.agent.instrumentation.micronaut.NRTokenRunnableWrapper;

import java.util.concurrent.Callable;

@Weave(originalName = "io.micronaut.core.propagation.PropagatedContext", type = MatchType.Interface)
public class PropagatedContext_Instrumentation {

    public Runnable wrap(Runnable runnable) {
        Runnable original = Weaver.callOriginal();
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if (transaction == null) {
            return original;
        }
        Token token = transaction.getToken();
        if (token == null) {
            return original;
        }
        return new NRTokenRunnableWrapper(original, token);
    }

    public <V> Callable<V> wrap(Callable<V> callable) {
        Callable<V> original = Weaver.callOriginal();
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if (transaction == null) {
            return original;
        }
        Token token = transaction.getToken();
        if (token == null) {
            return original;
        }
        return new NRTokenCallableWrapper<>(original, token);
    }
}
