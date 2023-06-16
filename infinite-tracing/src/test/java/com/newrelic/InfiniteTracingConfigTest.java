package com.newrelic;

import com.newrelic.api.agent.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    public void builder_withAllAttributes_successfullyConstructsObj() {
        InfiniteTracingConfig config = InfiniteTracingConfig.builder().useCompression(true)
                .flakyCode(1L)
                .flakyPercentage(.99)
                .host("foo.com")
                .licenseKey("123456")
                .port(9999)
                .maxQueueSize(10)
                .useBatching(true)
                .usePlaintext(true)
                .logger(Mockito.mock(Logger.class)).build();

        assertTrue(config.getUseCompression());
        assertTrue(config.getUseBatching());
        assertTrue(config.getUsePlaintext());
        assertEquals(1L, config.getFlakyCode());
        assertEquals(.99, config.getFlakyPercentage());
        assertEquals("foo.com", config.getHost());
        assertEquals("123456", config.getLicenseKey());
        assertEquals(9999, config.getPort());
        assertEquals(10, config.getMaxQueueSize());
        assertNotNull(config.getLogger());

    }
}
