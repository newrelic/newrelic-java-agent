package com.newrelic;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ChannelFactoryTest {

    private InfiniteTracingConfig mockConfig;

    @BeforeEach
    void setup() {
        mockConfig = mock(InfiniteTracingConfig.class);
        when(mockConfig.getHost()).thenReturn("localhost");
        when(mockConfig.getPort()).thenReturn(8989);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void shouldInjectAllAppropriateValues() {
        ChannelFactory target = new ChannelFactory(mockConfig, new HeadersInterceptor[0]);

        ManagedChannel channel = target.createChannel();
        assertNotNull(channel);

        verify(mockConfig).getHost();
        verify(mockConfig).getPort();
    }

    @Test
    void testDefaultConfigUsesSSL() {

        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .host("a")
                .port(3)
                .build();
        final OkHttpChannelBuilder builder = mock(OkHttpChannelBuilder.class);
        ManagedChannel channel = mock(ManagedChannel.class);

        when(builder.defaultLoadBalancingPolicy("pick_first")).thenReturn(builder);
        when(builder.intercept(any(ClientInterceptor[].class))).thenReturn(builder);
        when(builder.enableRetry()).thenReturn(builder);
        when(builder.defaultServiceConfig(isA(Map.class))).thenReturn(builder);
        when(builder.build()).thenReturn(channel);

        ChannelFactory target = new ChannelFactory(config, new HeadersInterceptor[0]) {
            @Override
            OkHttpChannelBuilder newOkHttpChannelBuilder() {
                return builder;
            }
        };

        ManagedChannel resultChannel = target.createChannel();
        assertSame(channel, resultChannel);
        verify(builder).useTransportSecurity();
        verify(builder, never()).usePlaintext();
    }
}