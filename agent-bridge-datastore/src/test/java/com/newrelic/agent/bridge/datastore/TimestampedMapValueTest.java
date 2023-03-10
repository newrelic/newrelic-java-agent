package com.newrelic.agent.bridge.datastore;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimestampedMapValueTest {

    TimestampedMapValue<String> mapValue = null;

    @Before
    public void setUp() {
        mapValue = new TimestampedMapValue<>("Claptrap");
    }

    @Test
    public void constructor_ShouldInitializeTimeCreatedAndTimeLastAccessed() {
        long testTs = System.currentTimeMillis();

        assertTrue(mapValue.getTimeCreated() - testTs <= 0);
        assertTrue(mapValue.getTimeLastAccessed() - testTs <= 0);
    }

    @Test
    public void getValue_ShouldReturnCorrectWrappedValue() {
        assertEquals("Claptrap", mapValue.getValue());
    }

    @Test
    public void getValue_ShouldUpdateLastAccessedTime() throws InterruptedException {
        long originalLastAccessedTs = mapValue.getTimeLastAccessed();

        TimeUnit.MILLISECONDS.sleep(10);
        mapValue.getValue();

        assertTrue((originalLastAccessedTs - mapValue.getTimeLastAccessed()) < 0);
    }

    @Test
    public void lastAccessedAfter_ShouldRespondProperlyBasedOnSuppliedInstant() {
        assertTrue(mapValue.lastAccessedAfter(System.currentTimeMillis() - 1000));
        assertFalse(mapValue.lastAccessedAfter(System.currentTimeMillis() + 1000));

        // Per the underlying API, equal values will return false
        assertFalse(mapValue.lastAccessedAfter(mapValue.getTimeLastAccessed()));
    }

    @Test
    public void lastAccessedPriorTo_ShouldRespondProperlyBasedOnSuppliedInstant() {
        assertFalse(mapValue.lastAccessedPriorTo(System.currentTimeMillis() -1000));
        assertTrue(mapValue.lastAccessedPriorTo(System.currentTimeMillis() + 1000));

        // Per the underlying API, equal values will return false
        assertFalse(mapValue.lastAccessedPriorTo(mapValue.getTimeLastAccessed()));
    }
}
