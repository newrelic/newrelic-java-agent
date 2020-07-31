package com.newrelic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InfiniteTracingConfigTest {

    @Test
    void testNoFlaky() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder().build();
        assertNull(config.getFlakyPercentage());
    }

    @Test
    void testFlaky() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .flakyPercentage(12.1)
                .build();
        assertEquals(12.1, config.getFlakyPercentage());
    }

    @Test
    void testDefaultsToUsingSSL() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder().build();
        assertFalse(config.getUsePlaintext());
    }

}