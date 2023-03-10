package com.newrelic.agent.bridge.datastore;

import java.time.Instant;

/**
 * A class designed to wrap a map value in a facade that contains the creation time
 * and the time the value was last accessed. This wrapper object can then be used in
 * other Map/cache classes that can utilize these timestamps for specific
 * use cases (expiring map entries based on value age, for example).
 *
 * @param <V> the class type of the underlying value
 */
public class TimestampedMapValue<V> {

    private final V wrappedValue;

    private final long timeCreated;
    private long timeLastAccessed;

    /**
     * Construct a new TimestampedMapValue, wrapping the supplied value of type V.
     * @param wrappedValue the value to be wrapped
     */
    public TimestampedMapValue(V wrappedValue) {
        this.wrappedValue = wrappedValue;

        long currentTimeMs = System.currentTimeMillis();
        this.timeCreated = currentTimeMs;
        this.timeLastAccessed = currentTimeMs;
    }

    /**
     * Return the value wrapped by this TimestampedMapValue.
     *
     * @return the underlying, wrapped value
     */
    public V getValue() {
        timeLastAccessed = System.currentTimeMillis();
        return this.wrappedValue;
    }

    /**
     * Return the time, in milliseconds of when this TimestampedMapValue was created.
     *
     * @return the creation timestamp as an {@link Instant}
     */
    public long getTimeCreated() {
        return this.timeCreated;
    }

    /**
     * Return the time, in milliseconds of when the underlying value was last accessed.
     *
     * @return @return the value's last accessed timestamp as an {@link Instant}
     */
    public long getTimeLastAccessed() {
        return this.timeLastAccessed;
    }

    /**
     * Return true if the underlying value was last accessed after the supplied time.
     *
     * @param targetTime the time, in milliseconds to compare the last accessed time with
     *
     * @return true if the underlying value was last accessed after the supplied time,
     * false otherwise
     */
    public boolean lastAccessedAfter(long targetTime) {
        return targetTime < this.getTimeLastAccessed();
    }

    /**
     * Return true if the underlying value was last accessed before the supplied time.
     *
     * @param targetTime the time, in milliseconds to compare the last accessed time with
     *
     * @return if the underlying value was last accessed before the supplied time,
     * false otherwise
     */
    public boolean lastAccessedPriorTo(long targetTime) {
        return targetTime > this.getTimeLastAccessed();
    }
}
