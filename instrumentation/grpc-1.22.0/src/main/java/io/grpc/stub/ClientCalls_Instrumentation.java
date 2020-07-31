/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.stub;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.grpc.GrpcConfig;
import io.grpc.ClientCall_Instrumentation;

@Weave(originalName = "io.grpc.stub.ClientCalls")
public final class ClientCalls_Instrumentation {

    private static <ReqT, RespT> void startCall(ClientCall_Instrumentation<ReqT, RespT> call,
            ClientCall_Instrumentation.Listener<RespT> responseListener, boolean streamingResponse) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("gRPC", "External");
        call.segment = segment;
        Weaver.callOriginal();
        responseListener.segment = segment;
        responseListener.authority = call.authority;
        responseListener.methodDescriptor = call.methodDescriptor;
    }

    @Weave(originalName = "io.grpc.stub.ClientCalls$CallToStreamObserverAdapter")
    private static final class CallToStreamObserverAdapter<T> {

        private final ClientCall_Instrumentation<T, ?> call = Weaver.callOriginal();

        @Trace(async = true)
        public void onError(Throwable t) {
            if (call != null && call.segment != null) {
                call.segment.getTransaction().getToken().linkAndExpire();
            }
            if (GrpcConfig.errorsEnabled) {
                NewRelic.noticeError(t);
            }
            Weaver.callOriginal();
        }

        @Trace(async = true)
        public void onCompleted() {
            if (call != null && call.segment != null) {
                call.segment.getTransaction().getToken().linkAndExpire();
            }
            Weaver.callOriginal();
        }

    }

}

