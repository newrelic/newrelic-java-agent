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

    @NewField
    public boolean isBidiStreaming;

    @NewField
    public boolean responseSent;

    /**
     * Instrumentation for writeMessage - called when server sends a response message.
     * For BIDI streaming, we finalize and expire the token after the first message is sent,
     * which ends the transaction at the point of response completion.
     */
    @Trace(async = true)
    public void writeMessage(Object message) {
        // Call original method first to send the message
        Weaver.callOriginal();

        // For BIDI streaming, end transaction after first response is sent
        if (isBidiStreaming && !responseSent && token != null) {
            // Finalize the transaction with success status
            GrpcUtil.finalizeTransaction(token, Status.OK, new Metadata());

            // Expire the token to end the transaction
            token.expire();

            // Mark that response was sent and clear token
            responseSent = true;
            token = null;

            NewRelic.addCustomParameter("grpc.transaction_ended_on_response", true);
        }
    }

    @Trace(async = true)
    public void close(Status status, Metadata metadata) {
        // Only finalize if token exists (token will be null for BIDI streaming after response sent)
        if (token != null) {
            GrpcUtil.finalizeTransaction(token, status, metadata);
            GrpcUtil.setServerStreamResponseStatus(status);
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
        // Only finalize if token exists (token will be null for BIDI streaming after response sent)
        if (token != null) {
            GrpcUtil.finalizeTransaction(token, status, new Metadata());
            GrpcUtil.setServerStreamResponseStatus(status);
        }

        Weaver.callOriginal();

        if (token != null) {
            token.expire();
            token = null;
        }
    }

    public abstract String getAuthority();
}
