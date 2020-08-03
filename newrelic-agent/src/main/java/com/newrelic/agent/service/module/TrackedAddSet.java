/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.agent.interfaces.backport.Consumer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TrackedAddSet<T> implements Consumer<T> {
    // This contains every item ever seen by this set.
    private final Set<T> fullSet = new HashSet<>();

    // This contains only the items seen since the last reset.
    private Set<T> addedElementSet = new HashSet<>();

    private final Object lock = new Object();

    /**
     * Clears the new elements, returning only those.
     * @return the deltas since the last reset.
     */
    public Set<T> resetReturningAdded() {
        synchronized (lock) {
            Set<T> returnSet = addedElementSet;
            addedElementSet = new HashSet<>();
            return Collections.unmodifiableSet(returnSet);
        }
    }

    /**
     * Clears the new elements and returns all elements.
     * @return the full set of elements.
     */
    public Set<T> resetReturningAll() {
        synchronized (lock) {
            addedElementSet = new HashSet<>();
            return Collections.unmodifiableSet(new HashSet<>(fullSet));
        }
    }

    /**
     * Adds an element to this set. If it has not yet been seen, it's added to the set of new elements, as well.
     *
     * @param element Element to add
     */
    @Override
    public void accept(T element) {
        synchronized (lock) {
            if (element != null && fullSet.add(element)) {
                addedElementSet.add(element);
            }
        }
    }
}
