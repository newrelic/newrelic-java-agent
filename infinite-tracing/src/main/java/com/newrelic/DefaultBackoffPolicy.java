package com.newrelic;

import io.grpc.Status;

import java.util.concurrent.TimeUnit;

public class DefaultBackoffPolicy implements BackoffPolicy {

    private final int DEFAULT_BACKOFF_DELAY = 15;

    @Override
    public boolean shouldReconnect(Status status) {
        // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#unimplemented
        if (status != null) {
            return status.getCode() != Status.Code.UNIMPLEMENTED;
        }
        return true;
    }

    @Override
    public int duration() {
        return DEFAULT_BACKOFF_DELAY;
    }

    @Override
    public void backoff() {
        try {
            // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#other-errors-1
            TimeUnit.SECONDS.sleep(DEFAULT_BACKOFF_DELAY);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
