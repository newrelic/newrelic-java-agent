/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;
import com.newrelic.agent.tracing.DistributedTraceUtil;
import com.newrelic.agent.util.MinAwareQueue;
import com.newrelic.agent.util.NoOpQueue;
import com.newrelic.agent.util.SynchronizedMinAwareQueue;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedSamplingPriorityQueue<E extends PriorityAware> implements SamplingPriorityQueue<E> {

    private final String appName;
    private final String serviceName;
    private final MinAwareQueue<E> data;
    private final AtomicInteger numberOfTries = new AtomicInteger();
    private final AtomicInteger sampled;
    private final Comparator<E> comparator;
    private final int maximumSize;

    public DistributedSamplingPriorityQueue(int reservoirSize) {
        this("", "", reservoirSize, null);
    }

    public DistributedSamplingPriorityQueue(String appName, String serviceName, int reservoirSize) {
        this(appName, serviceName, reservoirSize, null);
    }

    public DistributedSamplingPriorityQueue(int reservoirSize, Comparator<E> comparator) {
        this("", "", reservoirSize, comparator);
    }

    public DistributedSamplingPriorityQueue(String appName, String serviceName, int reservoirSize, Comparator<E> comparator) {
        this.appName = appName;
        this.serviceName = serviceName;
        this.comparator = comparator == null ? (left, right) -> Float.compare(right.getPriority(), left.getPriority()) : comparator;
        this.data = createQueue(reservoirSize, this.comparator);
        this.sampled = new AtomicInteger(0);
        this.maximumSize = reservoirSize;
    }

    private MinAwareQueue<E> createQueue(int reservoirSize, Comparator<E> comparator) {
        if (reservoirSize <= 0) {
            return new NoOpQueue<>();
        } else {
            return new SynchronizedMinAwareQueue<>(reservoirSize, comparator);
        }
    }

    public void retryAll(DistributedSamplingPriorityQueue<E> source) {
        synchronized (source.data) {
            for (E element : source.data) {
                add(element);
            }
        }
    }

    @Override
    public void retryAll(SamplingPriorityQueue<E> source) {
        for (E element: source.asList()) {
            add(element);
        }
    }

    @Override
    public boolean isFull() {
        return data.size() == maximumSize;
    }

    @Override
    public float getMinPriority() {
        return data.isEmpty() ? 0.0f : data.peekLast().getPriority();
    }

    @Override
    public int getNumberOfTries() {
        return numberOfTries.get();
    }

    @Override
    public void incrementNumberOfTries() {
        numberOfTries.incrementAndGet();
    }

    @Override
    public boolean add(E element) {
        incrementNumberOfTries();
        boolean added = data.offer(element);
        if (added && DistributedTraceUtil.isSampledPriority(element.getPriority())) {
            sampled.incrementAndGet();
        }
        return added;
    }

    @Override
    public E peek() {
        return data.peek();
    }

    @Override
    public E poll() {
        return data.poll();
    }

    @Override
    public List<E> asList() {
        List<E> elements;
        synchronized (data) {
            elements = new ArrayList<>(data);
        }
        Collections.sort(elements, this.comparator);
        return elements;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public int getTotalSampledPriorityEvents(){
        return sampled.get();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public void clear() {
        data.clear();
    }

}
