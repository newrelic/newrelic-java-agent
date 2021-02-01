package com.newrelic;

import com.google.common.collect.ImmutableMap;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeadersInterceptorTest {

    @Test
    void interceptCall_Valid() {
        MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
        CallOptions callOptions = mock(CallOptions.class);
        Channel next = mock(Channel.class);
        MockForwardingClientCall newCallNext = new MockForwardingClientCall();
        ClientCall.Listener<Object> responseListener = new ClientCall.Listener<Object>() {
        };

        when(next.newCall(method, callOptions)).thenReturn(newCallNext);

        Map<String, String> headers = ImmutableMap.of(
                "header1", "value1",
                "WILL_BE_LOWER_CASED", "value2");
        HeadersInterceptor target = new HeadersInterceptor(headers);

        ClientCall<Object, Object> result = target.interceptCall(method, callOptions, next);

        Metadata originalHeaders = InternalMetadata.newMetadata();
        originalHeaders.put(keyFor("k1"), "some value");

        result.start(responseListener, originalHeaders);
        assertEquals("some value", newCallNext.getHeader(keyFor("k1")));
        assertEquals("value1", newCallNext.getHeader(keyFor("header1")));
        assertEquals("value2", newCallNext.getHeader(keyFor("will_be_lower_cased")));
    }

    private static Metadata.Key<String> keyFor(String key) {
        return Metadata.Key.of(key, ASCII_STRING_MARSHALLER);
    }

    static class MockForwardingClientCall extends ForwardingClientCall<Object, Object> {
        private final List<Metadata> seenHeaders = new ArrayList<>();

        @Override
        public void start(Listener<Object> responseListener, Metadata headers) {
            seenHeaders.add(headers);
            super.start(responseListener, headers);
        }

        @Override
        protected ClientCall<Object, Object> delegate() {
            return mock(ClientCall.class);
        }

        public String getHeader(Metadata.Key<String> key) {
            return seenHeaders.get(0).get(key);
        }

    }

}