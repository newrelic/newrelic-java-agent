package com.newrelic;

import io.grpc.Status;

public interface BackoffPolicy {
    boolean shouldReconnect(Status status);
    int duration();
    void backoff();
}
