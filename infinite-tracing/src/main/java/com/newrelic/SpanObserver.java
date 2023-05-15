package com.newrelic;

import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;

import javax.annotation.Nullable;

public class SpanObserver implements Observer {

    private final ClientCallStreamObserver<V1.Span> observer;

    public SpanObserver(ClientCallStreamObserver<V1.Span> observer) {
        this.observer = observer;
    }

    @Override
    public void onNext(V1.Span span) {
        observer.onNext(span);
    }

    @Override
    public void onNext(V1.SpanBatch spanBatch) {
        // This should only be used by the SpanBatchObserver
        throw new UnsupportedOperationException();
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
