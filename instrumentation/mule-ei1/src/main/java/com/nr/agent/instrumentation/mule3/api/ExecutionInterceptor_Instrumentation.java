/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.api.execution.ExecutionCallback;

/**
 * This modules exists to instrument this particular version of this single class, and will load with versions of Mule
 * in the ranges [3.4.0,3.5.4) and [3.6.0,3.6.3). This design was chosen to reduce cope duplication and having to create
 * additional modules to handle two versions of 3.5 and 3.6.
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.execution.ExecutionInterceptor")
abstract class ExecutionInterceptor_Instrumentation<T> {

    @Trace(excludeFromTransactionTrace = true)
    public T execute(ExecutionCallback<T> callback) {
        return Weaver.callOriginal();
    }

}
