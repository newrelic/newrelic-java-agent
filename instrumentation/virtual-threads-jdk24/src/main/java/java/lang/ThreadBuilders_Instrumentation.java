/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package java.lang;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.virtualthreads.VirtualThreadUtils;

import java.util.concurrent.Executor;

@Weave(originalName = "java.lang.ThreadBuilders")
class ThreadBuilders_Instrumentation {
    static Thread newVirtualThread(Executor scheduler, String name, int characteristics, Runnable task) {
        if (VirtualThreadUtils.isSupportedRuntime) {
            Runnable wrapped = VirtualThreadUtils.getWrapper(task);
            if (wrapped != null) {
                task = wrapped;
            }
        }

        return Weaver.callOriginal();
    }
}