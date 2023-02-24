package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
     * Static executor to handle all {@code ExpiringValueCache} instances across the agent.
     * A single thread executor is adequate since the jobs run very infrequently and are
     * quick executing.
     */
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * Create a new ExpiringValueCache with the specified timer interval and expiration
     * logic function that will determine if an entry should be removed from the cache.
     *
     * <pre>
     *     // Example: This is our threshold for expiring an entry - anything not accessed within
     *     // 10 seconds ago will be evicted.
     *     long expireThreshold = System.currentTime.currentTimeMillis();
     *     ExpiringValueCache.ExpiringValueLogicFunction<Integer> func =
     *         (createdTime, timeLastAccessed) -> timeLastAccessed < (System.currentTimeMillis - expireThreshold);
     * </pre>
     * @param taskName an identifier given to the instance; used in log messages
     * @param timerIntervalMilli the interval time, in milliseconds, of how often the timer fires to check for items
     *                           to be evicted
     * @param expireLogicFunction the {@link ExpiringValueLogicFunction} lambda that contains the
     *                            logic to determine if an entry should be removed from the map
     */
    public ExpiringValueCache(String taskName, long timerIntervalMilli, ExpiringValueLogicFunction expireLogicFunction) {
        wrappedMap = new ConcurrentHashMap<>();

        Runnable task = () -> {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "ExpiringValueCache task [{0}] firing", taskName);

            wrappedMap.forEach((key, val) -> {
                if (expireLogicFunction.shouldExpireValue(val.getTimeCreated(), val.getTimeLastAccessed())) {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST, "Removing key [{0}] from cache [{}]", key, taskName);
                    wrappedMap.remove(key);
                }
            });
        };

        //The initial fire delay will just be the supplied interval for simplicity
        executorService.scheduleAtFixedRate(task, timerIntervalMilli, timerIntervalMilli, TimeUnit.MILLISECONDS);

        AgentBridge.getAgent().getLogger().log(Level.INFO, "ExpiringValueCache instance with timer: [{0}], " +
                "timer interval: {1}ms created", taskName, timerIntervalMilli);
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
        boolean shouldExpireValue(long createdTime, long timeLastAccessed);
    }
}
