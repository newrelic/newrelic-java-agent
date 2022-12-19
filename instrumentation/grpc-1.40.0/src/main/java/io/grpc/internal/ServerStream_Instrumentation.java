/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.internal;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.grpc.GrpcConfig;
import com.nr.agent.instrumentation.grpc.GrpcResponse;
import com.nr.agent.instrumentation.grpc.GrpcUtil;
import io.grpc.Metadata;
import io.grpc.Status;

// This class follows a request through the server side so we can hook in here to capture the outgoing request
@Weave(type = MatchType.Interface, originalName = "io.grpc.internal.ServerStream")
public abstract class ServerStream_Instrumentation {

    @NewField
    public Token token;

    @Trace(async = true)
    public void close(Status status, Metadata metadata) {
        GrpcUtil.finalizeTransaction(token, status, metadata);
        GrpcUtil.setServerStreamResponseStatus(status);

        Weaver.callOriginal();

        if (token != null) {
            token.expire();
            token = null;
        }
    }

    // server had an internal error
    @Trace(async = true)
    public void cancel(Status status) {
        GrpcUtil.finalizeTransaction(token, status, new Metadata());
        GrpcUtil.setServerStreamResponseStatus(status);

        Weaver.callOriginal();

        if (token != null) {
            token.expire();
            token = null;
        }
    }

    public abstract String getAuthority();
}
