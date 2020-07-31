package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlakyHeaderInterceptorTest {

    private static final Metadata.Key<String> K_KEY = Metadata.Key.of("k1", ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> FLAKY_KEY = Metadata.Key.of("flaky", ASCII_STRING_MARSHALLER);
    Metadata originalHeaders = InternalMetadata.newMetadata();

    @BeforeEach
    void setup() {
        originalHeaders = InternalMetadata.newMetadata();
        originalHeaders.put(K_KEY, "v1");
    }

    @Test
    void testInjectHeader() {
        final Double flakyValue = 15.6;
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .logger(mock(Logger.class))
                .flakyPercentage(flakyValue)
                .build();

        MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
        CallOptions callOptions = mock(CallOptions.class);
        Channel next = mock(Channel.class);
        MockForwardingClientCall newCallNext = new MockForwardingClientCall();
        ClientCall.Listener<Object> responseListener = new ClientCall.Listener<Object>() {
        };

        when(next.newCall(method, callOptions)).thenReturn(newCallNext);

        FlakyHeaderInterceptor testClass = new FlakyHeaderInterceptor(config);

        ClientCall<Object, Object> result = testClass.interceptCall(method, callOptions, next);

        result.start(responseListener, originalHeaders);
        assertEquals(1, newCallNext.seenHeadersSize());
        assertEquals("v1", newCallNext.getHeader(K_KEY));
        assertEquals("15.6", newCallNext.getHeader(FLAKY_KEY));
    }

    @Test
    void testNotConfigured() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder().build();

        MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
        CallOptions callOptions = mock(CallOptions.class);
        Channel next = mock(Channel.class);
        MockForwardingClientCall newCallNext = new MockForwardingClientCall();
        ClientCall.Listener<Object> responseListener = new ClientCall.Listener<Object>() {
        };

        when(next.newCall(method, callOptions)).thenReturn(newCallNext);

        FlakyHeaderInterceptor testClass = new FlakyHeaderInterceptor(config);

        ClientCall<Object, Object> result = testClass.interceptCall(method, callOptions, next);

        result.start(responseListener, originalHeaders);
        assertEquals(1, newCallNext.seenHeadersSize());
        assertEquals("v1", newCallNext.getHeader(K_KEY));
        assertFalse(newCallNext.containsKey(FLAKY_KEY));
    }

}