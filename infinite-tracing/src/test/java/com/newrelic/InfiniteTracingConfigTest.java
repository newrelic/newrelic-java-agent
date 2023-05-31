package com.newrelic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testEnableCompression() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .useCompression(true)
                .build();
        assertTrue(config.getUseCompression());
    }

    @Test
    void testDisableCompression() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .useCompression(false)
                .build();
        assertFalse(config.getUseCompression());
    }

    @Test
    void testEnableBatching() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .useBatching(true)
                .build();
        assertTrue(config.getUseBatching());
    }
}
