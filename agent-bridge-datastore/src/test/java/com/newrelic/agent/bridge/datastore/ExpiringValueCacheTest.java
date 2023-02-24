package com.newrelic.agent.bridge.datastore;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;

import org.junit.Test;

import java.time.Duration;

public class ExpiringValueCacheTest {

    private static final String VALUE_PREFIX = "val";

    @Test
    public void constructor_WithNoOpLambda_RemovesNoItems() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        int mapSize = testMap.size();

        //Let the timer fire a few times and make sure the map doesn't change during that time
        await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(4))
                .until(() -> testMap.size() == mapSize);
    }

    @Test
    public void timer_WithValidLambda_ExpiresTargetRecords() {
        ExpiringValueCache.ExpiringValueLogicFunction func =
                (createdTime, timeLastAccessed) -> timeLastAccessed < System.currentTimeMillis() + 10000;

        ExpiringValueCache<String, String> testMap = generateNewHashMap(1000, func);

        await()
                .atMost(3, SECONDS)
                .until(() -> testMap.size() == 0);
    }

    @Test
    public void timer_WithValidLambda_LeavesMapUnmodified() {
        ExpiringValueCache.ExpiringValueLogicFunction func =
                (createdTime, timeLastAccessed) -> timeLastAccessed < System.currentTimeMillis() - 10000;
        ExpiringValueCache<String, String> testMap = generateNewHashMap(1000, func);
        int mapSize = testMap.size();

        await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(4))
                .until(() -> testMap.size() == mapSize);
    }

    @Test
    public void containsKey_WithValidKey_ReturnsTrue() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        assertTrue(testMap.containsKey("1"));
    }

    @Test
    public void size_ReturnsProperValue() {
        ExpiringValueCache<String, String> testMap = new ExpiringValueCache<>("testTimer", 1000, noOpLogicFunction());
        assertEquals(0, testMap.size());

        testMap.put("1", "Borderlands");
        testMap.put("2", "Wonderlands");
        assertEquals(2, testMap.size());
    }

    @Test
    public void getValue_WithValidKey_ReturnsProperValue() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        assertEquals(VALUE_PREFIX + "1", testMap.get("1"));
    }

    @Test
    public void getValue_WithInvalidKey_ReturnsNull() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        assertNull(testMap.get("zzzzzzz"));
    }

    @Test
    public void putValue_WithValidKeyAndValue_AddsSuccessfully() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        int beforeMapSize = testMap.size();
        testMap.put("222", "Borderlands");

        assertEquals(beforeMapSize + 1, testMap.size());
    }

    @Test(expected = NullPointerException.class)
    public void putValue_WithNullKey_ThrowsException() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        testMap.put(null, "Borderlands");
    }

    @Test(expected = NullPointerException.class)
    public void putValue_WithNullValue_ThrowsException() {
        ExpiringValueCache<String, String> testMap = generateNewHashMap(500, noOpLogicFunction());
        testMap.put("222", null);
    }

    private ExpiringValueCache<String, String> generateNewHashMap(
            long timerInterval, ExpiringValueCache.ExpiringValueLogicFunction func) {

        ExpiringValueCache<String, String> testMap = new ExpiringValueCache<>("testTimer", timerInterval, func);
        for(int i=0; i< 20; i++) {
            testMap.put(Integer.toString(i), VALUE_PREFIX + i);
        }

        return testMap;
    }

    private ExpiringValueCache.ExpiringValueLogicFunction noOpLogicFunction() {
        return (created, accessed) -> false;
    }

}
