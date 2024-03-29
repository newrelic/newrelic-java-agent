/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.stub;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.grpc.GrpcConfig;
import io.grpc.ClientCall_Instrumentation;

@Weave(originalName = "io.grpc.stub.ClientCalls")
public final class ClientCalls_Instrumentation {

    private static <ReqT, RespT> void startCall(
            ClientCall_Instrumentation<ReqT, RespT> call,
            ClientCalls_Instrumentation.StartableListener_Instrumentation<RespT> responseListener) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("gRPC", "External");
        call.segment = segment;
        Weaver.callOriginal();
    }

    @Weave(type = MatchType.BaseClass, originalName = "io.grpc.stub.ClientCalls$StartableListener")
    private abstract static class StartableListener_Instrumentation<T> extends ClientCall_Instrumentation.Listener {
        abstract void onStart();
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

