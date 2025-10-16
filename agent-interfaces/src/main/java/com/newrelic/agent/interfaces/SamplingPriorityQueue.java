/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.interfaces;

import com.newrelic.agent.model.PriorityAware;

import java.util.List;

public interface SamplingPriorityQueue<E extends PriorityAware> {
    void retryAll(SamplingPriorityQueue<E> source);

    boolean isFull();

    float getMinPriority();

    int getNumberOfTries();

    void incrementNumberOfTries();

    boolean add(E element);

    E peek();

    E poll();

    List<E> asList();

    String getAppName();

    String getServiceName();

    int getTotalSampledPriorityEvents();

    int size();

    void clear();

    void logReservoirStats();
}
