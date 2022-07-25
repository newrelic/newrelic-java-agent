package com.newrelic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.newrelic.BackoffPolicy.BACKOFF_SECONDS_SEQUENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BackoffPolicyTest {

    private BackoffPolicy target;

    @BeforeEach
    void setup() {
        target = new BackoffPolicy();
    }

    @Test
    void getNextBackoffSeconds_Valid() {
        List<Integer> responses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            responses.add(target.getNextBackoffSeconds());
        }

        assertEquals(BACKOFF_SECONDS_SEQUENCE[0], responses.get(0));
        assertEquals(BACKOFF_SECONDS_SEQUENCE[BACKOFF_SECONDS_SEQUENCE.length - 1], responses.get(responses.size() - 1));

        target.reset();

        assertEquals(BACKOFF_SECONDS_SEQUENCE[0], target.getNextBackoffSeconds());
    }

}