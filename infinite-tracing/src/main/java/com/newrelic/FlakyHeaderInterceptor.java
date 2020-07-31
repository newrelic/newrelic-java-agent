package com.newrelic;

import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata.Key;

import java.util.logging.Level;

/**
 * Injects the "flaky" header on each sent span, only if configured to
 * do so.
 */
public class FlakyHeaderInterceptor implements ClientInterceptor {

    private static final Key<String> FLAKY_HEADER = Key.of("flaky", Metadata.ASCII_STRING_MARSHALLER);
    private final InfiniteTracingConfig config;

    public FlakyHeaderInterceptor(InfiniteTracingConfig config) {
        this.config = config;
        if (config.getFlakyPercentage() != null) {
            config.getLogger().log(Level.WARNING, "Infinite tracing is configured with a flaky percentage!  There will be errors!");
        }
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        final Double flakyPercentage = config.getFlakyPercentage();
        if (flakyPercentage == null) {
            return next.newCall(method, callOptions);
        }
        return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(FLAKY_HEADER, flakyPercentage.toString());
                super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }

}
