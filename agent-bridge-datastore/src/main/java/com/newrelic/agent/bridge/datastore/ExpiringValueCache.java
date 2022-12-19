package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * A cache implementation that uses a composition pattern to wrap a {@link ConcurrentHashMap}
 * populated with {@link TimestampedMapValue} instances. An instance of this class is constructed
 * with function that contains the logic of when to evict items from the cache.
 *
 * @param <K> the class type of the cache key
 * @param <V> the class type of the value object
 */
public class ExpiringValueCache<K, V> {

    private final ConcurrentHashMap<K, TimestampedMapValue<V>> wrappedMap;

    /**
     * Create a new ExpiringValueCache with the specified timer interval and expiration
     * logic function that will determine if an entry should be removed from the cache.
     *
     * <pre>
     *     // Example: This is our threshold for expiring an entry - anything not accessed within
     *     // 10 seconds ago will be evicted.
     *     Instant expireThreshold = Instant.now().minusMillis(10000)
     *     ExpiringValueCache.ExpiringValueLogicFunction<Integer> func =
     *         (createdTime, timeLastAccessed) -> timeLastAccessed.isBefore(Instant.now().minusMillis(expireThreshold));
     * </pre>
     * @param timerThreadName an identifier given to the instance; used in log messages
     * @param timerIntervalMilli the interval time, in milliseconds, of how often the timer fires to check for items
     *                           to be evicted
     * @param expireLogicFunction the {@link ExpiringValueLogicFunction} lambda that contains the
     *                            logic to determine if an entry should be removed from the map
     */
    public ExpiringValueCache(String timerThreadName, long timerIntervalMilli,
            ExpiringValueLogicFunction expireLogicFunction) {
        wrappedMap = new ConcurrentHashMap<>();

        TimerTask task = new TimerTask() {
            public void run() {
                AgentBridge.getAgent().getLogger().log(Level.FINE, "ExpiringValueCache timer [{0}] firing", timerThreadName);

                wrappedMap.forEach((key, val) -> {
                    if (expireLogicFunction.shouldExpireValue(val.getTimeCreated(), val.getTimeLastAccessed())) {
                        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Removing key [{0}] from cache [{}]", key, timerThreadName);
                        wrappedMap.remove(key);
                    }
                });
            }
        };

        //The initial fire delay will just be the supplied interval for simplicity
        Timer timer = new Timer(timerThreadName, true);
        timer.scheduleAtFixedRate(task, timerIntervalMilli, timerIntervalMilli);

        AgentBridge.getAgent().getLogger().log(Level.INFO, "ExpiringValueCache instance with timer: [{0}], " +
                "timer interval: {1}ms created", timerThreadName, timerIntervalMilli);
    }

    /**
     * Return the value mapped by the supplied key.
     *
     * @param key the key for the target value
     * @return the target value
     */
    public V get(K key) {
        TimestampedMapValue<V> testValue = wrappedMap.get(key);
        return testValue == null ? null : testValue.getValue();
    }

    /**
     * Add a new value to the map.
     *
     * @param key the key for the supplied value
     * @param value the value to add to the map, wrapped in a TimestampedMapValue instance
     */
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        wrappedMap.put(key, new TimestampedMapValue<>(value));
        return value;
    }

    public boolean containsKey(K key) {
        return wrappedMap.containsKey(key);
    }

    public int size() {
        return wrappedMap.size();
    }

    /**
     * A functional interface for implementing the logic to determine if an entry in the map should
     * be removed based on the supplied {@link TimestampedMapValue} instance.
     */
    @FunctionalInterface
    public interface ExpiringValueLogicFunction {
        boolean shouldExpireValue(Instant createdTime, Instant timeLastAccessed);
    }
}
