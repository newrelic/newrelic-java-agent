/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.grpc.GrpcRequest;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;

@Weave(originalName = "io.grpc.internal.ServerImpl")
public class ServerImpl_Instrumentation {

    @Weave(originalName = "io.grpc.internal.ServerImpl$ServerTransportListenerImpl")
    private static final class ServerTransportListenerImpl_Instrumentation {

        @Trace(dispatcher = true)
        private <ReqT, RespT> ServerStreamListener startCall(ServerStream_Instrumentation stream, String fullMethodName,
                ServerMethodDefinition<ReqT, RespT> methodDef, Metadata headers,
                Context.CancellableContext context, StatsTraceContext statsTraceCtx) {
            MethodDescriptor<ReqT, RespT> methodDescriptor = methodDef.getMethodDescriptor();
            NewRelic.getAgent().getTransaction().setWebRequest(new GrpcRequest(fullMethodName, stream.getAuthority(), headers));

            stream.token = AgentBridge.getAgent().getTransaction().getToken();

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
