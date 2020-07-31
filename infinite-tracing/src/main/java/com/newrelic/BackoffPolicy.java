package com.newrelic;

import io.grpc.Status;

public interface BackoffPolicy {
    boolean shouldReconnect(Status status);
    void backoff();
}
