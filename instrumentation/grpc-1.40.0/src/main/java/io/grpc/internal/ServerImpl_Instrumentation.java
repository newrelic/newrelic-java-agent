/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.grpc.GrpcRequest;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.perfmark.Tag;

@Weave(originalName = "io.grpc.internal.ServerImpl")
public class ServerImpl_Instrumentation {

    @Weave(originalName = "io.grpc.internal.ServerImpl$ServerTransportListenerImpl")
    private static final class ServerTransportListenerImpl_Instrumentation {

        @NewField
        private Metadata headers;

        private void streamCreatedInternal(
                final ServerStream stream, final String methodName, final Metadata headers, final Tag tag) {
            this.headers = headers;

            Weaver.callOriginal();
        }

        @Trace(dispatcher = true)
        private <ReqT, RespT> ServerMethodDefinition<?, ?> wrapMethod(ServerStream_Instrumentation stream,
                ServerMethodDefinition<ReqT, RespT> methodDef,
                StatsTraceContext statsTraceCtx) {
            MethodDescriptor<ReqT, RespT> methodDescriptor = methodDef.getMethodDescriptor();
            String fullMethodName = methodDescriptor != null ? methodDescriptor.getFullMethodName() : null;

            // Check if this is a BIDI streaming method
            boolean isBidiStreaming = methodDescriptor != null &&
                methodDescriptor.getType() == MethodDescriptor.MethodType.BIDI_STREAMING;

            // Always create transaction and set web request
            NewRelic.getAgent().getTransaction().setWebRequest(new GrpcRequest(fullMethodName, stream.getAuthority(), this.headers));
            stream.token = AgentBridge.getAgent().getTransaction().getToken();

            // Mark BIDI streaming for special handling (transaction ends on first response)
            if (isBidiStreaming) {
                stream.isBidiStreaming = true;
                NewRelic.addCustomParameter("grpc.transaction_ends_on_response", true);
            }

            if (fullMethodName != null && !fullMethodName.isEmpty()) {
                NewRelic.addCustomParameter("request.method", fullMethodName);
                NewRelic.getAgent().getTracedMethod().setMetricName("gRPC", "ServerCallHandler", "startCall", fullMethodName);
                NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "gRPC", fullMethodName);

                if (methodDescriptor != null) {
                    NewRelic.addCustomParameter("grpc.type", methodDescriptor.getType().name());
                }
            }

            return Weaver.callOriginal();
        }
    }

}
