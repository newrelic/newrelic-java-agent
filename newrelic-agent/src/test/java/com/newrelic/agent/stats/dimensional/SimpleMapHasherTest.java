package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class SimpleMapHasherTest extends TestCase {
    @Test
    public void testHashEmptyMap() {
        assertEquals(Long.MAX_VALUE, SimpleMapHasher.INSTANCE.hash(ImmutableMap.of()));
    }

    @Test
    public void testHashMapOneItem() {
        assertEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5)));
        assertNotEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 7)));
        assertNotEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test2", 5)));

        assertEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5L)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5L)));
        assertEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5f)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5f)));
        assertEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5d)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5d)));

        assertNotEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5L)));
    }

    @Test
    public void testHashMap() {
        assertEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5, "test2", 8)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5, "test2", 8)));
        assertEquals(SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test", 5, "test2", 8L)), SimpleMapHasher.INSTANCE.hash(ImmutableMap.of("test2", 8L, "test", 5)));
    }

}