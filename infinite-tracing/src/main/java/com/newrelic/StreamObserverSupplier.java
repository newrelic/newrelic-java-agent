package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;

public class StreamObserverSupplier implements Supplier<ClientCallStreamObserver<V1.Span>> {

    private final Supplier<ManagedChannel> channelSupplier;
    private final Function<ManagedChannel, ClientCallStreamObserver<V1.Span>> channelToStreamObserverConverter;

    public StreamObserverSupplier(Supplier<ManagedChannel> channelSupplier,
            Function<ManagedChannel, ClientCallStreamObserver<V1.Span>> channelToStreamObserverConverter) {
        this.channelSupplier = channelSupplier;
        this.channelToStreamObserverConverter = channelToStreamObserverConverter;
    }

    @Override
    public ClientCallStreamObserver<V1.Span> get() {
        return channelToStreamObserverConverter.apply(channelSupplier.get());
    }
}
