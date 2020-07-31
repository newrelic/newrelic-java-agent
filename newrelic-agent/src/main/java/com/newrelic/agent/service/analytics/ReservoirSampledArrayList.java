/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A ReserviorSampledArrayList is constrained in size and will not grow above that size. Overflow is managed by the
 * reservoir algorithm. Slots in the indexed list can be heuristically pre-allocated by calling getSlot(). If it returns
 * null, the caller need not construct the object that would be placed in the slot. Otherwise, the caller should
 * construct the object and attempt to store it in the slot. The store may fail e.g. because the slot got
 * double-allocated or the collection got harvested in the meantime, and the caller has no way to know. But these
 * failures only occur when the array is full and some events are being discarded anyway, thus
 * "heuristic pre-allocation." The implementation must be thread safe and preferably lock-free.
 */
public class ReservoirSampledArrayList<E> extends FixedSizeArrayList<E> {

    public ReservoirSampledArrayList(int reservoirSize) {
        super(reservoirSize);
    }

    @Override
    public Integer getSlot() {
        int currentCount = numberOfTries.incrementAndGet() - 1;
        int insertIndex;
        if (currentCount < size) {
            insertIndex = currentCount;
        } else {
            insertIndex = ThreadLocalRandom.current().nextInt(currentCount);
        }
        if (insertIndex >= size) {
            return null;
        }
        return insertIndex;
    }

    void setRandomFixedSeed(long seed) {
        ThreadLocalRandom.current().setSeed(seed);
    }
}
