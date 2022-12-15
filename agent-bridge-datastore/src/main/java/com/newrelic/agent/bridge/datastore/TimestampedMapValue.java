package com.newrelic.agent.bridge.datastore;

import java.time.Instant;

/**
 * A class designed to wrap a map value in a facade that contains the creation time
 * and the time the value was last accessed. This wrapper object can then be placed in
 * a concrete {@link java.util.Map} class that can utilize these timestamps for specific
 * use cases (expiring map entries based on value age, for example).
 *
 * @param <V> the class type of the underlying value
 */
public class TimestampedMapValue<V> {

    private final V wrappedValue;

    private final Instant timeCreated;
    private Instant timeLastAccessed;

    /**
     * Construct a new TimestampedMapValue, wrapping the supplied value of type V.
     * @param wrappedValue the value to be wrapped
     */
    public TimestampedMapValue(V wrappedValue) {
        this.wrappedValue = wrappedValue;

        Instant currentTime = Instant.now();
        this.timeCreated = currentTime;
        this.timeLastAccessed = currentTime;
    }

    /**
     * Return the value wrapped by this TimestampedMapValue.
     *
     * @return the underlying, wrapped value
     */
    public V getValue() {
        timeLastAccessed = Instant.now();
        return this.wrappedValue;
    }

    /**
     * Return the {@link Instant} of when this TimestampedMapValue was created.
     *
     * @return the creation timestamp as an {@link Instant}
     */
    public Instant getTimeCreated() {
        return this.timeCreated;
    }

    /**
     * Return the {@link Instant} of when the underlying value was last accessed.
     *
     * @return @return the value's last accessed timestamp as an {@link Instant}
     */
    public Instant getTimeLastAccessed() {
        return this.timeLastAccessed;
    }

    /**
     * Return true if the underlying value was last accessed after the supplied time.
     *
     * @param targetTime the {@link Instant} to compare the last accessed time with
     *
     * @return true if the underlying value was last accessed after the supplied time,
     * false otherwise
     */
    public boolean lastAccessedAfter(Instant targetTime) {
        return this.timeLastAccessed.isAfter(targetTime);
    }

    /**
     * Return true if the underlying value was last accessed before the supplied time.
     *
     * @param targetTime the {@link Instant} to compare the last accessed time with
     *
     * @return if the underlying value was last accessed before the supplied time,
     * false otherwise
     */
    public boolean lastAccessedPriorTo(Instant targetTime) {
        return this.timeLastAccessed.isBefore(targetTime);
    }
}
