package com.newrelic;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

class MockForwardingClientCall extends ForwardingClientCall<Object, Object> {
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

    public int seenHeadersSize() {
        return seenHeaders.size();
    }

    public String getHeader(Metadata.Key<String> key) {
        return seenHeaders.get(0).get(key);
    }

    public boolean containsKey(Metadata.Key<String> key) {
        return seenHeaders.get(0).containsKey(key);
    }
}
