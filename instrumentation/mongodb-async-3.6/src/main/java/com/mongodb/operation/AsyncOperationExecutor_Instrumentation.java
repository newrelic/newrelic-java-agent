/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.operation;

import com.mongodb.ReadPreference;
import com.mongodb.async.SingleResultCallback;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "com/mongodb/operation/AsyncOperationExecutor")
public abstract class AsyncOperationExecutor_Instrumentation {

    @Trace
    public <T> void execute(AsyncReadOperation<T> operation, ReadPreference readPreference, SingleResultCallback<T> callback) {
        NewRelic.getAgent().getTracedMethod().setMetricName(new String[] { "Custom", "AsyncOperationExecutor", "Read", "execute" });
        Weaver.callOriginal();
    }

    @Trace
    public <T> void execute(AsyncWriteOperation<T> operation, SingleResultCallback<T> callback) {
        NewRelic.getAgent().getTracedMethod().setMetricName(new String[] { "Custom", "AsyncOperationExecutor", "Write", "execute" });
        Weaver.callOriginal();
    }
}
