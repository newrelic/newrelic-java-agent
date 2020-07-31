/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * A timed set interface that should act like a set that can time out values. The time out period is set during
 * creation, and all entries adhere to it.
 *
 * Note that:
 * 1. The time out policy should be expire after last access, although it is not enforced.
 * 2. It is not required that getting the size of the collection is an exact value, as is the case with a guava cache.
 * 3. Any changes to how this cache does expiration also needs to be made consistent with AsyncTransactionService.
 *
 * @param <K> type of the object being stored
 */
interface TimedSet<K> {
    /**
     * The exact number of entries that were removed due to hitting a time out limit.
     */
    int timedOutCount();

    /**
     * Add a value to the set.
     */
    void put(K value);

    /**
     * Manually remove a value before it times out. This should not increment the time out count.
     */
    boolean remove(K value);

    /**
     * Manually remove all values before their time outs. This should not increment the time out count.
     */
    void removeAll();

    /**
     * A utility method that some implementations may require, in order to force the collection to time out and remove
     * entries. For example, guava caches do not use a thread to monitor entries to time them out, but does incremental
     * clean up work during normal operation. This may synchronize on entire the collection.
     */
    void cleanUp();

    /**
     * Refresh the last access time of a token.
     */
    void refresh(TokenImpl token);
}
