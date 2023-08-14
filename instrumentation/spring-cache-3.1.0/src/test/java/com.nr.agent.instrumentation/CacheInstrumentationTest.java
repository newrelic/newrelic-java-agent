package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.cache" })
public class CacheInstrumentationTest {
    @Test
    public void get_withCacheMiss_generatesMissMetric() {
        SampleCacheImpl cacheImpl = new SampleCacheImpl();
        cacheImpl.get(null);    //Simulate cache miss via null param

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Supportability/Spring/Cache/java.util.HashMap/sample/Misses"));
    }

    @Test
    public void get_withCacheHit_generatesHitMetric() {
        SampleCacheImpl cacheImpl = new SampleCacheImpl();
        cacheImpl.get("foo");    //Simulate cache hit

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Supportability/Spring/Cache/java.util.HashMap/sample/Hits"));
    }

    @Test
    public void get_withCacheClear_generatesClearMetric() {
        SampleCacheImpl cacheImpl = new SampleCacheImpl();
        cacheImpl.clear();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Supportability/Spring/Cache/java.util.HashMap/sample/Clear"));
    }

    @Test
    public void get_withCacheEvict_generatesEvictMetric() {
        SampleCacheImpl cacheImpl = new SampleCacheImpl();
        cacheImpl.evict("foo");

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getUnscopedMetrics().containsKey("Supportability/Spring/Cache/java.util.HashMap/sample/Evict"));
    }
}
