package com.newrelic;

import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBackoffPolicyTest {
    @Test
    public void shouldReconnectOnMostErrors() {
        DefaultBackoffPolicy target = new DefaultBackoffPolicy();

        assertTrue(target.shouldReconnect(null));
        assertTrue(target.shouldReconnect(Status.OK));
        assertTrue(target.shouldReconnect(Status.UNAVAILABLE));
        assertTrue(target.shouldReconnect(Status.CANCELLED));
        assertTrue(target.shouldReconnect(Status.DEADLINE_EXCEEDED));
        assertTrue(target.shouldReconnect(Status.UNAUTHENTICATED));
    }

    @Test
    public void shouldNotReconnectOnUnimplemented() {
        DefaultBackoffPolicy target = new DefaultBackoffPolicy();

        assertFalse(target.shouldReconnect(Status.UNIMPLEMENTED));
    }

}