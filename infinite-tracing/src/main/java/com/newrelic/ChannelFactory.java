package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

public class ChannelFactory {
    private final InfiniteTracingConfig config;
    private final ClientInterceptor[] interceptors;

    public ChannelFactory(InfiniteTracingConfig config, ClientInterceptor... interceptors) {
        this.config = config;
        this.interceptors = interceptors;
    }

    public ManagedChannel createChannel() {
        OkHttpChannelBuilder okHttpChannelBuilder = newOkHttpChannelBuilder()
                .defaultLoadBalancingPolicy("pick_first")
                .intercept(interceptors);

        if (config.getUsePlaintext()) {
            okHttpChannelBuilder.usePlaintext();
        } else {
            okHttpChannelBuilder.useTransportSecurity();
        }
        return okHttpChannelBuilder.build();
    }

    @VisibleForTesting
    OkHttpChannelBuilder newOkHttpChannelBuilder() {
        return OkHttpChannelBuilder
                .forAddress(config.getHost(), config.getPort());
    }

}
