/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.client;

import com.newrelic.api.agent.HttpParameters;
import kotlin.coroutines.Continuation;

import java.util.HashSet;
import java.util.Set;

public class KtorClientUtils {

    private static final Set<String> LEAF_CLIENTS = new HashSet<>();

    static {
        LEAF_CLIENTS.add("JavaHttpEngine");
        LEAF_CLIENTS.add("CIOEngine");
    }

    public static boolean needsLeaf(String engine) {
        return LEAF_CLIENTS.contains(engine);
    }

    public static <T> NRContinuationWrapper<T> getContinuationWrapper(Continuation<T> continuation, HttpParameters httpParameters) {
        if(continuation instanceof NRContinuationWrapper) {
            return null;
        }
        return new NRContinuationWrapper<>(continuation, httpParameters);
    }

}
