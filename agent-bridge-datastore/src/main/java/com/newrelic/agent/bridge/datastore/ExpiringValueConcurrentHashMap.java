package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 *
 * @param <K> the class type of the map key
 * @param <V> the class type of the value object
 */
public class ExpiringValueConcurrentHashMap<K, V> extends ConcurrentHashMap<K, TimestampedMapValue<V>> {

    /**
     * Create a new ExpiringValueConcurrentHashMap with the specified timer interval and expiration
     * logic function that will determine if an entry should be removed from the map.
     *
     * <pre>
     *     // This is our threshold for expiring an entry - anything not accessed within
     *     // 10 seconds ago will be evicted.
     *     Instant expireThreshold = Instant.now().minusMillis(10000)
     *     ExpiringValueConcurrentHashMap.ExpiringValueLogicFunction<Integer> func =
     *         (val) -> val.lastAccessedPriorTo(expireThreshold);
     * </pre>
     *
     * @param timerIntervalMilli the interval time, in milliseconds, of how often the timer fires
     * @param expireLogicFunction the {@link ExpiringValueLogicFunction} lambda that contains the
     *                            logic to determine if an entry should be removed from the map,
     *                            utilizing the supplied {@link TimestampedMapValue} instance
     */
    public ExpiringValueConcurrentHashMap(String timerThreadName, long timerIntervalMilli,
            ExpiringValueLogicFunction<V> expireLogicFunction) {
        TimerTask task = new TimerTask() {
            public void run() {
                // Any value that hasn't been accessed after this time will be evicted from the map
                //Instant expireThreshold = Instant.now().minusMillis(lastAccessedThresholdMilli);
                AgentBridge.getAgent().getLogger().log(Level.FINE, "ExpiringValueConcurrentHashMap timer [{0}] firing", timerThreadName);

                forEach((key, val) -> {
                    if (expireLogicFunction.shouldExpireValue(val)) {
                        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Removing key [{0}] from map", key);
                        remove(key);
                    }
                });
            }
        };

        //The initial fire delay will just be the supplied interval for simplicity
        Timer timer = new Timer(timerThreadName, true);
        timer.scheduleAtFixedRate(task, timerIntervalMilli, timerIntervalMilli);

        AgentBridge.getAgent().getLogger().log(Level.INFO, "ExpiringValueConcurrentHashMap instance with timer: [{0}], " +
                "timer interval: {1}ms created", timerThreadName, timerIntervalMilli);
    }

    /**
     * Return the underlying wrapped value in the {@link TimestampedMapValue} mapped by the supplied key.
     *
     * @param key the key for the target value
     * @return the target value
     */
    public V getUnwrappedValue(K key) {
        TimestampedMapValue<V> testValue = get(key);
        return testValue == null ? null : testValue.getValue();
    }

    /**
     * A convenience method for adding a new value to the map without having to construct a {@link TimestampedMapValue}
     * manually.
     *
     * @param key the key for the supplied value
     * @param value the value to add to the map, wrapped in a TimestampedMapValue instance
     */
    public void putUnwrappedValue(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        put(key, new TimestampedMapValue<>(value));
    }

    /**
     * A functional interface for implementing the logic to determine if an entry in the map should
     * be removed based on the supplied {@link TimestampedMapValue} instance.
     *
     * @param <V> the class type of the underlying, wrapped value
     */
    @FunctionalInterface
    public interface ExpiringValueLogicFunction<V> {
        boolean shouldExpireValue(TimestampedMapValue<V> valueToCheck);
    }
}
