package com.newrelic;

import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Accepts a {@link ManagedChannel} and provides a valid {@link ClientCallStreamObserver}, creating it only when required.
 *
 * Not thread-safe.
 */
public class ChannelToStreamObserver implements Function<ManagedChannel, ClientCallStreamObserver<V1.Span>> {
    private final StreamObserverFactory streamObserverFactory;
    private final AtomicBoolean shouldRecreateCall;
    private volatile ManagedChannel lastChannel;
    private volatile ClientCallStreamObserver<V1.Span> streamObserver;

    public ChannelToStreamObserver(
            StreamObserverFactory streamObserverFactory,
            AtomicBoolean shouldRecreateCall) {
        this.streamObserverFactory = streamObserverFactory;
        this.shouldRecreateCall = shouldRecreateCall;
    }

    @Override
    public ClientCallStreamObserver<V1.Span> apply(ManagedChannel channel) {
        if (channel == null) {
            return null;
        }

        if (lastChannel != channel || streamObserver == null || shouldRecreateCall.get()) {
            recreateStreamObserver(channel);
        }

        return streamObserver;
    }

    private void recreateStreamObserver(ManagedChannel channel) {
        lastChannel = channel;
        clearStreamObserver();
        streamObserver = streamObserverFactory.buildStreamObserver(channel);
        shouldRecreateCall.set(false);
    }

    private void clearStreamObserver() {
        if (this.streamObserver != null) {
            ClientCallStreamObserver<V1.Span> oldStreamObserver = this.streamObserver;
            this.streamObserver = null;
            oldStreamObserver.cancel("CLOSING_CONNECTION", new ChannelClosingException());
        }
    }
}
