package com.newrelic;

import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;

import javax.annotation.Nullable;

public class SpanBatchObserver implements Observer {

    private final ClientCallStreamObserver<V1.SpanBatch> observer;

    public SpanBatchObserver(ClientCallStreamObserver<V1.SpanBatch> observer) {
        this.observer = observer;
    }

    @Override
    public void onNext(V1.Span span) {
        // This should only be used by the SpanObserver
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNext(V1.SpanBatch spanBatch) {
        observer.onNext(spanBatch);
    }

    @Override
    public boolean isReady() {
        return observer.isReady();
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
        observer.cancel(message, cause);
    }
}
