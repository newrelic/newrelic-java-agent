package com.newrelic;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.Map;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

class HeadersInterceptor implements ClientInterceptor {

    private final Map<String, String> headers;

    HeadersInterceptor(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new HeadersClientCall<>(method, callOptions, next);
    }

    private class HeadersClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

        protected HeadersClientCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            super(next.newCall(method, callOptions));
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata metadata) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                metadata.put(Metadata.Key.of(header.getKey().toLowerCase(), ASCII_STRING_MARSHALLER), header.getValue());
            }
            super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                @Override
                public void onHeaders(Metadata headers) {
                    super.onHeaders(headers);
                }
            }, metadata);
        }
    }
}
