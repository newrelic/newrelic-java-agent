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
 * ExecutionTemplate provides an execution context for message processing.
 * <p>
 * Examples of execution context can be to provide error handling, transaction state verification,
 * transactional demarcation.
 *
 * @param <T> type of the return value of the processing execution
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.api.execution.ExecutionTemplate")
public abstract class ExecutionTemplate_Instrumentation<T> {

    @Trace(excludeFromTransactionTrace = true)
    public T execute(ExecutionCallback<T> callback) {
        return Weaver.callOriginal();
    }

}
