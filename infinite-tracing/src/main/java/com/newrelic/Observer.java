package com.newrelic;

import com.newrelic.trace.v1.V1;

import javax.annotation.Nullable;

/**
 * Shared interface between batching and non-batching trace observer implementations
 */
public interface Observer {

    /**
     * Sends a single span to the observer
     */
    void onNext(V1.Span span);

    /**
     * Sends a batch of spans to the observer.
     */
    void onNext(V1.SpanBatch spanBatch);

    /**
     * Whether the observer is in a ready state to accept spans
     */
    boolean isReady();

    /**
     * Cancel the connection to the observer
     */
    void cancel(@Nullable String message, @Nullable Throwable cause);

}
