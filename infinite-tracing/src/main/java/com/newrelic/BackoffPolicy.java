package com.newrelic;

import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.atomic.AtomicInteger;

class BackoffPolicy {

    @VisibleForTesting
    static final int[] BACKOFF_SECONDS_SEQUENCE = new int[] { 15, 15, 30, 60, 120, 300 };
    private static final int DEFAULT_BACKOFF_SECONDS = 15;

    private final AtomicInteger backoffSequenceIndex = new AtomicInteger(-1);

    /**
     * Get the default backoff seconds.
     *
     * @return the default backoff seconds
     */
    int getDefaultBackoffSeconds() {
        return DEFAULT_BACKOFF_SECONDS;
    }

    /**
     * Get the next entry in the backoff sequence.
     *
     * @return the next number of seconds to backoff
     */
    int getNextBackoffSeconds() {
        int nextIndex = backoffSequenceIndex.incrementAndGet();
        return nextIndex < BACKOFF_SECONDS_SEQUENCE.length
                ? BACKOFF_SECONDS_SEQUENCE[nextIndex]
                : BACKOFF_SECONDS_SEQUENCE[BACKOFF_SECONDS_SEQUENCE.length - 1];
    }

    /**
     * Reset the backoff sequence.
     */
    void reset() {
        backoffSequenceIndex.set(-1);
    }

}