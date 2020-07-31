/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.errorhandler;

import java.io.IOException;

import com.newrelic.agent.bridge.Instrumentation;
import org.objectweb.asm.tree.ClassNode;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;

public final class LogAndReturnOriginal extends ErrorTrapHandler {
    public static final ClassNode ERROR_HANDLER_NODE;

    static {
        ClassNode result;
        try {
            result = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                    LogAndReturnOriginal.class.getName(), LogAndReturnOriginal.class.getClassLoader()));
        } catch (IOException e) {
            result = null;
        }

        ERROR_HANDLER_NODE = result;
    }

    /**
     * Call {@link Instrumentation#noticeInstrumentationError(Throwable, String)} and return the original value.
     */
    public static void onWeaverThrow(Throwable weaverError) throws Throwable {
        AgentBridge.instrumentation.noticeInstrumentationError(weaverError, Weaver.getImplementationTitle());
    }
}
