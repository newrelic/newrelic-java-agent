/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

import java.util.concurrent.atomic.AtomicBoolean;

@Weave(originalName = "reactor.core.publisher.Hooks")
public abstract class Hooks_Instrumentation {

    /*
     * Note that sub-hooks are cumulative. We want to avoid setting the same sub-hooks
     * more than once, so we set this boolean to true the first time we set a sub-hook.
     * if (!Hooks_Instrumentation.instrumented.getAndSet(true)) { Hooks.onEachOperator(...) }
     */
    @NewField
    public static AtomicBoolean instrumented = new AtomicBoolean(false);
}
