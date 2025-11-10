/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.grpc.GrpcConfig;
import com.nr.agent.instrumentation.grpc.InboundHeadersWrapper;
import com.nr.agent.instrumentation.grpc.OutboundHeadersWrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "io.grpc.ClientCall")
public class ClientCall_Instrumentation<ReqT, RespT> {

    @NewField
    public Segment segment = null;

    @NewField
    public String authority = null;

    @NewField
    public MethodDescriptor<ReqT, RespT> methodDescriptor;

    public void start(Listener<RespT> responseListener, Metadata headers) {
        if (segment != null) {
            responseListener.segment = segment;
            responseListener.authority = authority;
            responseListener.methodDescriptor = methodDescriptor;
            OutboundHeadersWrapper wrapper = new OutboundHeadersWrapper(headers);
            segment.addOutboundRequestHeaders(wrapper);
        }
        Weaver.callOriginal();
    }

    public void halfClose() {
        Weaver.callOriginal();
        cleanUpNewFields();
    }

    public void cancel(String message, Throwable cause) {
        if (GrpcConfig.errorsEnabled && cause != null) {
            NewRelic.noticeError(cause);
        }
        if (segment != null) {
            segment.end();
        }
        Weaver.callOriginal();
        cleanUpNewFields();
    }

    // This is important to keep here in order to avoid a memory leak
    private void cleanUpNewFields() {
        this.segment = null;
        this.authority = null;
        this.methodDescriptor = null;
    }

    @Weave(type = MatchType.BaseClass, originalName = "io.grpc.ClientCall$Listener")
    public abstract static class Listener<T> {

        @NewField
        public Segment segment = null;
        @NewField
        public String authority;
        @NewField
        public MethodDescriptor methodDescriptor;
        @NewField
        private Metadata headers = null;

        public void onHeaders(Metadata headers) {
            this.headers = headers;
            Weaver.callOriginal();
        }

        public void onClose(Status status, Metadata trailers) {
            if (GrpcConfig.errorsEnabled && status.getCause() != null) {
                // If an error occurred during the close of this call we should record it
                NewRelic.noticeError(status.getCause());
            }

            Weaver.callOriginal();
            InboundHeadersWrapper wrapper = new InboundHeadersWrapper(headers, trailers);

            URI uri = null;
            if (authority != null && methodDescriptor != null) {
                try {
                    uri = new URI("grpc", authority, "/" + methodDescriptor.getFullMethodName(), null, null);
                } catch (URISyntaxException e) {
                    AgentBridge.getAgent().getLogger().log(Level.FINER, "Exception with uri: " + e.getMessage());
                }
                if (uri != null) {
                    ExternalParameters params = HttpParameters.library("gRPC")
                            .uri(uri)
                            .procedure(methodDescriptor.getFullMethodName())
                            .inboundHeaders(wrapper)
                            .status(status.getCode().value(), status.getDescription())
                            .build();
                    if (segment != null) {
                        segment.reportAsExternal(params);
                        segment.end();
                    }
                }
            }

            // This is important to keep here in order to avoid a memory leak
            this.segment = null;
            this.headers = null;
            this.authority = null;
            this.methodDescriptor = null;
        }
    }
}
