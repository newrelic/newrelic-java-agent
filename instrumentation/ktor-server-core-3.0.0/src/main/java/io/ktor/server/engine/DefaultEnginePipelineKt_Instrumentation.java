/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.engine;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.server.application.ApplicationCall;
import kotlin.coroutines.Continuation;

@Weave(originalName = "io.ktor.server.engine.DefaultEnginePipelineKt")
public class DefaultEnginePipelineKt_Instrumentation {

    public static Object handleFailure(ApplicationCall call, Throwable cause, Continuation<?> continuation) {
        NewRelic.noticeError(cause);
        return Weaver.callOriginal();
    }
}
