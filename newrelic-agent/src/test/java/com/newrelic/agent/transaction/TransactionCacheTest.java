/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;
import com.newrelic.agent.util.Caffeine2CollectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

// AI assisted test generation
public class TransactionCacheTest {

    private TransactionCache cache;

    @Before
    public void setUp() {
        // Set up a real collection factory that supports weak keys
        AgentBridge.collectionFactory = new Caffeine2CollectionFactory();
        cache = new TransactionCache();
    }

    @After
    public void tearDown() {
        // Reset to avoid affecting other tests
        AgentBridge.collectionFactory = new com.newrelic.agent.bridge.DefaultCollectionFactory();
    }

    @Test
    public void testPutAndGetURL() throws MalformedURLException {
        Object key = new Object();
        URL url = new URL("https://example.com");

        cache.putURL(key, url);

        assertEquals("Should retrieve the same URL", url, cache.getURL(key));
    }

    @Test
    public void testGetURLWithNonExistentKey() {
        Object key = new Object();

        assertNull("Should return null for non-existent key", cache.getURL(key));
    }

    @Test
    public void testOverwriteURL() throws MalformedURLException {
        Object key = new Object();
        URL url1 = new URL("https://example.com");
        URL url2 = new URL("https://newrelic.com");

        cache.putURL(key, url1);
        assertEquals("Should retrieve first URL", url1, cache.getURL(key));

        cache.putURL(key, url2);
        assertEquals("Should retrieve second URL after overwrite", url2, cache.getURL(key));
    }

    @Test
    public void testMultipleURLEntries() throws MalformedURLException {
        Object key1 = new Object();
        Object key2 = new Object();
        Object key3 = new Object();

        URL url1 = new URL("https://example1.com");
        URL url2 = new URL("https://example2.com");
        URL url3 = new URL("https://example3.com");

        cache.putURL(key1, url1);
        cache.putURL(key2, url2);
        cache.putURL(key3, url3);

        assertEquals("Should retrieve URL for key1", url1, cache.getURL(key1));
        assertEquals("Should retrieve URL for key2", url2, cache.getURL(key2));
        assertEquals("Should retrieve URL for key3", url3, cache.getURL(key3));
    }

    @Test
    public void testURLCacheLazyInitialization() throws MalformedURLException {
        // Create a new cache (urlCache should be null initially)
        TransactionCache newCache = new TransactionCache();

        Object key = new Object();
        URL url = new URL("https://example.com");

        // First put should trigger lazy initialization
        newCache.putURL(key, url);
        assertEquals("Should work after lazy initialization", url, newCache.getURL(key));
    }

    @Test
    public void testURLCacheWithDifferentKeyTypes() throws MalformedURLException {
        String stringKey = "stringKey";
        Integer intKey = 123;
        Object objectKey = new Object();

        URL url1 = new URL("https://example1.com");
        URL url2 = new URL("https://example2.com");
        URL url3 = new URL("https://example3.com");

        cache.putURL(stringKey, url1);
        cache.putURL(intKey, url2);
        cache.putURL(objectKey, url3);

        assertEquals("Should work with String key", url1, cache.getURL(stringKey));
        assertEquals("Should work with Integer key", url2, cache.getURL(intKey));
        assertEquals("Should work with Object key", url3, cache.getURL(objectKey));
    }

    @Test
    public void testPutAndGetMetricNameFormatWithHost() {
        Object key = new Object();
        MetricNameFormatWithHost format = mock(MetricNameFormatWithHost.class);

        cache.putMetricNameFormatWithHost(key, format);

        assertEquals("Should retrieve the same format", format, cache.getMetricNameFormatWithHost(key));
    }

    @Test
    public void testGetMetricNameFormatWithHostForNonExistentKey() {
        Object key = new Object();

        assertNull("Should return null for non-existent key", cache.getMetricNameFormatWithHost(key));
    }

    @Test
    public void testOverwriteMetricNameFormatWithHost() {
        Object key = new Object();
        MetricNameFormatWithHost format1 = mock(MetricNameFormatWithHost.class);
        MetricNameFormatWithHost format2 = mock(MetricNameFormatWithHost.class);

        cache.putMetricNameFormatWithHost(key, format1);
        assertEquals("Should retrieve first format", format1, cache.getMetricNameFormatWithHost(key));

        cache.putMetricNameFormatWithHost(key, format2);
        assertEquals("Should retrieve second format after overwrite", format2, cache.getMetricNameFormatWithHost(key));
    }

    @Test
    public void testMultipleMetricNameFormatWithHostEntries() {
        Object key1 = new Object();
        Object key2 = new Object();
        Object key3 = new Object();

        MetricNameFormatWithHost format1 = mock(MetricNameFormatWithHost.class);
        MetricNameFormatWithHost format2 = mock(MetricNameFormatWithHost.class);
        MetricNameFormatWithHost format3 = mock(MetricNameFormatWithHost.class);

        cache.putMetricNameFormatWithHost(key1, format1);
        cache.putMetricNameFormatWithHost(key2, format2);
        cache.putMetricNameFormatWithHost(key3, format3);

        assertEquals("Should retrieve format for key1", format1, cache.getMetricNameFormatWithHost(key1));
        assertEquals("Should retrieve format for key2", format2, cache.getMetricNameFormatWithHost(key2));
        assertEquals("Should retrieve format for key3", format3, cache.getMetricNameFormatWithHost(key3));
    }

    @Test
    public void testMetricNameFormatWithHostCacheLazyInitialization() {
        // Create a new cache (inputStreamCache should be null initially)
        TransactionCache newCache = new TransactionCache();

        Object key = new Object();
        MetricNameFormatWithHost format = mock(MetricNameFormatWithHost.class);

        // First put should trigger lazy initialization
        newCache.putMetricNameFormatWithHost(key, format);
        assertEquals("Should work after lazy initialization", format, newCache.getMetricNameFormatWithHost(key));
    }

    @Test
    public void testURLAndMetricCachesAreIndependent() throws MalformedURLException {
        Object key = new Object();
        URL url = new URL("https://example.com");
        MetricNameFormatWithHost format = mock(MetricNameFormatWithHost.class);

        // Put different values in both caches with same key
        cache.putURL(key, url);
        cache.putMetricNameFormatWithHost(key, format);

        // Both should be retrievable independently
        assertEquals("Should retrieve URL from URL cache", url, cache.getURL(key));
        assertEquals("Should retrieve format from metric cache", format, cache.getMetricNameFormatWithHost(key));
    }

    @Test
    public void testBothCachesCanBeUsedSimultaneously() throws MalformedURLException {
        Object urlKey1 = new Object();
        Object urlKey2 = new Object();
        Object metricKey1 = new Object();
        Object metricKey2 = new Object();

        URL url1 = new URL("https://example1.com");
        URL url2 = new URL("https://example2.com");
        MetricNameFormatWithHost format1 = mock(MetricNameFormatWithHost.class);
        MetricNameFormatWithHost format2 = mock(MetricNameFormatWithHost.class);

        cache.putURL(urlKey1, url1);
        cache.putURL(urlKey2, url2);
        cache.putMetricNameFormatWithHost(metricKey1, format1);
        cache.putMetricNameFormatWithHost(metricKey2, format2);

        assertEquals("URL cache should have entry 1", url1, cache.getURL(urlKey1));
        assertEquals("URL cache should have entry 2", url2, cache.getURL(urlKey2));
        assertEquals("Metric cache should have entry 1", format1, cache.getMetricNameFormatWithHost(metricKey1));
        assertEquals("Metric cache should have entry 2", format2, cache.getMetricNameFormatWithHost(metricKey2));
    }

    @Test
    public void testSameObjectCanBeUsedAsBothKeyAndValue() throws MalformedURLException {
        URL url = new URL("https://example.com");

        // Use URL as both key and value
        cache.putURL(url, url);

        assertEquals("Should retrieve URL when URL is used as key", url, cache.getURL(url));
    }
}