/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.util.pipeline;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.utils.PipelineUtils;
import kotlin.coroutines.Continuation;

@Weave(type = MatchType.BaseClass, originalName = "io.ktor.util.pipeline.Pipeline")
public class Pipeline_Instrumentation<TSubject, TContext> {

    @NewField
    private Token token;

    public Pipeline_Instrumentation(PipelinePhase... phases) {
        if (token != null) {
            return;
        }
        String simpleName = getClass().getSimpleName();
        if (PipelineUtils.tracePipeline(simpleName)) {
            Token t = NewRelic.getAgent().getTransaction().getToken();
            if (t != null && t.isActive()) {
                token = t;
            } else if (t != null) {
                t.expire();
            }
        }
    }

    @Trace(async = true)
    public Object execute(TContext context, TSubject subject, Continuation<? super TSubject> continuation) {
        Token localToken = token;
        token = null;
        if (localToken != null) {
            localToken.linkAndExpire();
        }
        return Weaver.callOriginal();
    }



}
