/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.context;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "io.opentelemetry.context.Context")
public abstract class Context_Instrumentation {
    public static Context current() {
        return ContextHelper.current(Weaver.callOriginal());
    }

    public Scope makeCurrent() {
        return ContextHelper.makeCurrent((Context) this, Weaver.callOriginal());
    }
}
