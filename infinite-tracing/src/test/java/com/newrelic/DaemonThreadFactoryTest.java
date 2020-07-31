package com.newrelic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DaemonThreadFactoryTest {
    @Test
    public void createsDaemonThreads() {
        DaemonThreadFactory target = new DaemonThreadFactory("service name");
        Thread result1 = target.newThread(new NoOpRunnable());
        assertTrue(result1.isDaemon());
        assertEquals("New Relic service name #1", result1.getName());

        Thread result2 = target.newThread(new NoOpRunnable());
        assertTrue(result2.isDaemon());
        assertEquals("New Relic service name #2", result2.getName());
    }

    private static class NoOpRunnable implements Runnable {
        @Override
        public void run() {
        }
    }

}