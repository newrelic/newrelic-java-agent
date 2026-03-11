/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.reactive.execution;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.function.BiConsumer;

@Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.reactive.execution.ReactorExecutionFlowImpl")
abstract class ReactorExecutionFlowImpl_Instrumentation {

    @Trace
    public void onComplete(BiConsumer<? super Object, Throwable> fn) {
        Weaver.callOriginal();
    }
}
