/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.HashMap;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.cache" })
public class CacheInstrumentationTest {
    @Test
    public void get_withCacheMiss_generatesMissMetric() {
        Cache cacheImpl = new SampleCacheImpl();
        cacheImpl.get(null);    //Simulate cache miss via null param

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Cache/Spring/java.util.HashMap/sample/misses"));
    }

    @Test
    public void get_withNullCacheProvider_reportsProviderAsUnknown() {
        Cache cacheImpl = new SampleCacheWithoutProvider();
        cacheImpl.get(null);    //Simulate cache miss via null param

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Cache/Spring/Unknown/sample/misses"));
    }

    @Test
    public void get_withCacheHit_generatesHitMetric() {
        Cache cacheImpl = new SampleCacheImpl();
        cacheImpl.get("foo");    //Simulate cache hit

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Cache/Spring/java.util.HashMap/sample/hits"));
    }

    @Test
    public void get_withCacheClear_generatesClearMetric() {
        Cache cacheImpl = new SampleCacheImpl();
        cacheImpl.clear();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Cache/Spring/java.util.HashMap/sample/clear"));
    }

    @Test
    public void get_withCacheEvict_generatesEvictMetric() {
        Cache cacheImpl = new SampleCacheImpl();
        cacheImpl.evict("foo");

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Cache/Spring/java.util.HashMap/sample/evict"));
    }

    private static class SampleCacheImpl implements Cache {
        @Override
        public String getName() {
            return "sample";
        }

        @Override
        public Object getNativeCache() {
            return new HashMap<>();
        }

        @Override
        public ValueWrapper get(Object key) {
            //Simulate miss
            if (key == null) {
                return null;
            }

            //Simulate hit
            return new SimpleValueWrapper("foo");
        }

        @Override
        public void put(Object key, Object value) {

        }

        @Override
        public void evict(Object key) {

        }

        @Override
        public void clear() {

        }
    }

    private static class SampleCacheWithoutProvider extends SampleCacheImpl {
        @Override
        public Object getNativeCache() {
            return null;
        }
    }
}
