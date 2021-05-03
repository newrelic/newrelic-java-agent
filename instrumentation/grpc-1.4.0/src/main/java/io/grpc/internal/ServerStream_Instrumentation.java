/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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
import io.grpc.Metadata;
import io.grpc.Status;

// This class follows a request through the server side so we can hook in here to capture the outgoing request
@Weave(type = MatchType.Interface, originalName = "io.grpc.internal.ServerStream")
public abstract class ServerStream_Instrumentation {

    @NewField
    public Token token;

    @Trace(async = true)
    public void close(Status status, Metadata trailers) {
        if (token != null) {
            token.link();
            Transaction transaction = NewRelic.getAgent().getTransaction();
            transaction.setWebResponse(new GrpcResponse(status, trailers));
            transaction.addOutboundResponseHeaders();
            transaction.markResponseSent();
        }

        if (status != null) {
            int statusCode = status.getCode().value();
            NewRelic.addCustomParameter("response.status", statusCode);
            NewRelic.addCustomParameter("http.statusCode", statusCode);
            NewRelic.addCustomParameter("http.statusText", status.getDescription());
            if (GrpcConfig.errorsEnabled && status.getCause() != null) {
                // If an error occurred during the close of this server call we should record it
                NewRelic.noticeError(status.getCause());
            }
        }

        Weaver.callOriginal();

        if (token != null) {
            token.expire();
            token = null;
        }
    }

    // server had an internal error
    @Trace(async = true)
    public void cancel(Status status) {
        if (token != null) {
            token.link();
            Transaction transaction = token.getTransaction();
            transaction.setWebResponse(new GrpcResponse(status, new Metadata()));
            transaction.addOutboundResponseHeaders();
            transaction.markResponseSent();
        }

        if (status != null) {
            int statusCode = status.getCode().value();
            NewRelic.addCustomParameter("response.status", statusCode);
            NewRelic.addCustomParameter("http.statusCode", statusCode);
            NewRelic.addCustomParameter("http.statusText", status.getDescription());
            if (GrpcConfig.errorsEnabled && status.getCause() != null) {
                // If an error occurred during the close of this server call we should record it
                NewRelic.noticeError(status.getCause());
            }
        }

        Weaver.callOriginal();

        if (token != null) {
            token.expire();
            token = null;
        }
    }

    public abstract String getAuthority();
}
