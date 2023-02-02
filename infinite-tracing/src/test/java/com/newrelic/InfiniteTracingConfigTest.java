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
    void testGzipCompression() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .compression("gzip")
                .build();
        assertEquals("gzip", config.getCompression());
    }

    @Test
    void testDisableCompression() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .compression(null)
                .build();
        assertNull(config.getCompression());
    }

    @Test
    void testEnableBatching() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .useBatching(true)
                .build();
        assertTrue(config.getUseBatching());
    }

    @Test
    void testLingerMs() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .lingerMs(100)
                .build();
        assertEquals(100, config.getLingerMs());
    }
}
