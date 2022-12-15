package com.newrelic.agent.bridge.datastore;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

public class ExpiringValueConcurrentHashMapTest {

    private static final String VALUE_PREFIX = "val";

    @Test
    public void constructor_WithNoOpLambda_RemovesNoItems() {
        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        int mapSize = testMap.size();

        //Let the timer fire a few times and make sure the map doesn't change during that time
        await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(4))
                .until(() -> testMap.size() == mapSize);
    }

    @Test
    public void timer_WithValidLambda_ExpiresTargetRecords() {
        Instant expireThreshold = Instant.now().plusMillis(10000);
        ExpiringValueConcurrentHashMap.ExpiringValueLogicFunction<String> func = (val) -> val.lastAccessedPriorTo(expireThreshold);

        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(1000, func);

        await()
                .atMost(3, SECONDS)
                .until(() -> testMap.size() == 0);
    }

    public void timer_WithValidLambda_LeavesMapUnmodified() {
        Instant expireThreshold = Instant.now().minusMillis(10000);
        ExpiringValueConcurrentHashMap.ExpiringValueLogicFunction<String> func = (val) -> val.lastAccessedPriorTo(expireThreshold);

        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(1000, func);
        int mapSize = testMap.size();

        await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(4))
                .until(() -> testMap.size() == mapSize);
    }

    @Test
    public void getUnwrappedValue_WithValidKey_ReturnsProperValue() {
        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        assertEquals(VALUE_PREFIX + "1", testMap.getUnwrappedValue("1"));
    }

    @Test
    public void getUnwrappedValue_WithInvalidKey_ReturnsNull() {
        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        assertNull(testMap.getUnwrappedValue("zzzzzzz"));
    }

    @Test
    public void putUnwrappedValue_WithValidKeyAndValue_AddsSuccessfully() {
        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        int beforeMapSize = testMap.size();
        testMap.putUnwrappedValue("222", "Borderlands");

        assertEquals(beforeMapSize + 1, testMap.size());
    }

    @Test(expected = NullPointerException.class)
    public void putUnwrappedValue_WithNullKey_ThrowsException() {
        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        testMap.putUnwrappedValue(null, "Borderlands");
    }

    @Test(expected = NullPointerException.class)
    public void putUnwrappedValue_WithNullValue_ThrowsException() {
        ExpiringValueConcurrentHashMap<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        testMap.putUnwrappedValue("222", null);
    }

    private ExpiringValueConcurrentHashMap<String, String> generateNewHashMap(
            long timerInterval, ExpiringValueConcurrentHashMap.ExpiringValueLogicFunction<String> func) {

        ExpiringValueConcurrentHashMap<String, String> testMap = new ExpiringValueConcurrentHashMap<>("testTimer", timerInterval, func);
        for(int i=0; i< 20; i++) {
            testMap.put(Integer.toString(i), new TimestampedMapValue<>(VALUE_PREFIX + i));
        }

        return testMap;
    }

    private ExpiringValueConcurrentHashMap.ExpiringValueLogicFunction<String> noOpLogicFunction() {
        return (val) -> false;
    }

}
