package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.Map;

// this class is for adding headers to the outbound span event stream
public class HeadersInterceptor implements ClientInterceptor {

    private final Supplier<Map<String, String>> headersSupplier;

    public HeadersInterceptor(Supplier<Map<String, String>> headersSupplier) {
        this.headersSupplier = headersSupplier;
    }
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                for (Map.Entry<String, String> header: headersSupplier.get().entrySet()) {
                    headers.put(Metadata.Key.of(header.getKey().toLowerCase(), Metadata.ASCII_STRING_MARSHALLER), header.getValue());
                }
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}