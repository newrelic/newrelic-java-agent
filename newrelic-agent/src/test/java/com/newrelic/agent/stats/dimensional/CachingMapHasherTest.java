package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

public class CachingMapHasherTest extends TestCase {

    @Test
    public void testCacheMiss() {
        MapHasher mapper = Mockito.spy(SimpleMapHasher.INSTANCE);
        CachingMapHasher mapHasher = new CachingMapHasher(mapper);
        Map<String, Object> map = ImmutableMap.of("country", "Spain");
        assertEquals(-1715954995560262958l, mapHasher.hash(map));
        Mockito.verify(mapper, Mockito.times(1)).hash(map);
    }

    @Test
    public void testCacheHit() {
        MapHasher mapper = Mockito.mock(MapHasher.class);
        CachingMapHasher mapHasher = new CachingMapHasher(mapper);
        Map<String, Object> map = ImmutableMap.of("country", "Spain");
        mapHasher.addHash(map, 66l);
        assertEquals(66l, mapHasher.hash(map));
        Mockito.verify(mapper, Mockito.times(0)).hash(map);

        // clear the cache, causing the call to go through to the mock
        mapHasher.reset();
        assertEquals(0l, mapHasher.hash(map));
        Mockito.verify(mapper, Mockito.times(1)).hash(map);
    }
}