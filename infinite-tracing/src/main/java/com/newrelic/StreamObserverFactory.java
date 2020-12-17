package com.newrelic;

import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.IngestServiceGrpc;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;

public class StreamObserverFactory {
    private final Logger logger;
    private final MetricAggregator aggregator;
    private final StreamObserver<V1.RecordStatus> responseObserver;

    public StreamObserverFactory(Logger logger,
                                 MetricAggregator aggregator,
                                 StreamObserver<V1.RecordStatus> responseObserver) {
        this.logger = logger;
        this.aggregator = aggregator;
        this.responseObserver = responseObserver;
    }

    public ClientCallStreamObserver<V1.Span> buildStreamObserver(ManagedChannel channel) {
        IngestServiceGrpc.IngestServiceStub ingestServiceFutureStub = IngestServiceGrpc.newStub(channel);
        ClientCallStreamObserver<V1.Span> streamObserver = (ClientCallStreamObserver<V1.Span>) ingestServiceFutureStub.recordSpan(responseObserver);

        aggregator.incrementCounter("Supportability/InfiniteTracing/Connect");
        logger.log(Level.FINE, "Connected to Trace Observer.");
        return streamObserver;
    }

}
