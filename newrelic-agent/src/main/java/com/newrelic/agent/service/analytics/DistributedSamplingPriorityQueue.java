/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Queues;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;
import com.newrelic.agent.tracing.DistributedTraceUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedSamplingPriorityQueue<E extends PriorityAware> implements SamplingPriorityQueue<E> {

    private final String appName;
    private final String serviceName;
    private final Queue<E> data;
    private final AtomicInteger numberOfTries = new AtomicInteger();
    private final AtomicInteger recorded;
    // the number of times the decider was used on an event on this application. That meaning, the number of
    // events that started on this application that did not accept a payload.
    private final AtomicInteger decided;

    private final int decidedLast;
    private final int target;
    private final Comparator<E> comparator;
    private final int maximumSize;

    public DistributedSamplingPriorityQueue(int reservoirSize) {
        this("", "", reservoirSize, 0, 0, null);
    }

    public DistributedSamplingPriorityQueue(String appName, String serviceName, int reservoirSize) {
        this(appName, serviceName, reservoirSize, 0, 0, null);
    }

    public DistributedSamplingPriorityQueue(int reservoirSize, int decidedLast, int target) {
        this("", "", reservoirSize, decidedLast, target, null);
    }

    public DistributedSamplingPriorityQueue(String appName, String serviceName, int reservoirSize, int decidedLast, int target) {
        this(appName, serviceName, reservoirSize, decidedLast, target, null);
    }

    public DistributedSamplingPriorityQueue(int reservoirSize, int decidedLast, int target, Comparator<E> comparator) {
        this("", "", reservoirSize, decidedLast, target, comparator);
    }

    public DistributedSamplingPriorityQueue(String appName, String serviceName, int reservoirSize, int decidedLast, int target, Comparator<E> comparator) {
        this.appName = appName;
        this.serviceName = serviceName;
        this.comparator = comparator == null ? new Comparator<E>() {
            @Override
            public int compare(E left, E right) {
                return Float.compare(right.getPriority(), left.getPriority());
            }
        } : comparator;
        this.data = createQueue(reservoirSize, this.comparator);
        this.recorded = new AtomicInteger(0);
        this.decidedLast = decidedLast;
        this.target = target;
        this.decided = new AtomicInteger(0);
        this.maximumSize = reservoirSize;
    }

    private Queue<E> createQueue(int reservoirSize, Comparator<E> comparator) {
        if (reservoirSize <= 0) {
            return new NoOpQueue<>();
        } else {
            return Queues.synchronizedQueue(MinMaxPriorityQueue
                    .orderedBy(comparator)
                    .maximumSize(reservoirSize)
                    .create());
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
        return data.isEmpty() ? 0.0f : data.peek().getPriority();
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
        if (added && element.decider()) {
            decided.incrementAndGet();
            if (DistributedTraceUtil.isSampledPriority(element.getPriority())) {
                recorded.incrementAndGet();
            }
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
    public int getSampled() {
        return recorded.get();
    }

    @Override
    public int getDecided() {
        return decided.get();
    }

    @Override
    public int getTarget() {
        return target;
    }

    @Override
    public int getDecidedLast() {
        return decidedLast;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public void clear() {
        data.clear();
    }

    private static final class NoOpQueue<E extends PriorityAware> implements Queue<E> {
        @Override
        public boolean add(E e) {
            return false;
        }

        @Override
        public boolean offer(E e) {
            return false;
        }

        @Override
        public E remove() {
            return null;
        }

        @Override
        public E poll() {
            return null;
        }

        @Override
        public E element() {
            return null;
        }

        @Override
        public E peek() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public void remove() {
                }

                @Override
                public E next() {
                    return null;
                }
            };
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }
}
