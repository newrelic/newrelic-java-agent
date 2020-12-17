package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Status;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectBackoffPolicy implements BackoffPolicy {

    @VisibleForTesting
    final int[] BACKOFF_INTERVAL_IN_SEC = new int[] { 0, 15, 15, 30, 60, 120, 300 };
    private final int MAX_BACKOFF_DELAY = 300;
    private final AtomicInteger backoffIndex = new AtomicInteger(0);

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
        return BACKOFF_INTERVAL_IN_SEC [backoffIndex.get()];
    }

    @Override
    public void backoff() {
        try {
            // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#failed_precondition
            TimeUnit.SECONDS.sleep(nextBackoffDuration());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public void reset() {
        backoffIndex.set(0);
    }

    int nextBackoffDuration() {
        return BACKOFF_INTERVAL_IN_SEC [backoffIndex.get()] != MAX_BACKOFF_DELAY ?
                BACKOFF_INTERVAL_IN_SEC [backoffIndex.getAndIncrement()] : MAX_BACKOFF_DELAY;
    }
}
