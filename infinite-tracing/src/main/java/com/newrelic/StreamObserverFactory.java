package com.newrelic;

import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.IngestServiceGrpc;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class StreamObserverFactory {
    private final MetricAggregator aggregator;
    private final StreamObserver<V1.RecordStatus> responseObserver;

    public StreamObserverFactory(
            MetricAggregator aggregator,
            StreamObserver<V1.RecordStatus> responseObserver) {
        this.aggregator = aggregator;
        this.responseObserver = responseObserver;
    }

    public ClientCallStreamObserver<V1.Span> buildStreamObserver(ManagedChannel channel) {
        IngestServiceGrpc.IngestServiceStub ingestServiceFutureStub = IngestServiceGrpc.newStub(channel);
        ClientCallStreamObserver<V1.Span> streamObserver = (ClientCallStreamObserver<V1.Span>) ingestServiceFutureStub.recordSpan(responseObserver);

        aggregator.incrementCounter("Supportability/InfiniteTracing/Connect");
        return streamObserver;
    }

}
