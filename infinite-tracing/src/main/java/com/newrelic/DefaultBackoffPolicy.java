package com.newrelic;

import io.grpc.Status;

import java.util.concurrent.TimeUnit;

public class DefaultBackoffPolicy implements BackoffPolicy {

    @Override
    public boolean shouldReconnect(Status status) {
        // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#unimplemented
        return status != Status.UNIMPLEMENTED;
    }

    @Override
    public void backoff() {
        try {
            // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#other-errors-1
            TimeUnit.SECONDS.sleep(15);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
