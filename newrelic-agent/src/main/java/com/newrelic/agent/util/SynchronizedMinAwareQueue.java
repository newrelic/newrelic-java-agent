/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.google.common.collect.MinMaxPriorityQueue;
import com.newrelic.agent.model.PriorityAware;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;

public final class SynchronizedMinAwareQueue<E extends PriorityAware> implements MinAwareQueue<E> {
    private final MinMaxPriorityQueue<E> delegate;

    public SynchronizedMinAwareQueue(int reservoirSize, Comparator<E> comparator) {
        this.delegate = MinMaxPriorityQueue.orderedBy(comparator).maximumSize(reservoirSize).create();
    }

    //Used for reservoir
    @Override
    public synchronized boolean offer(E e) {
        return delegate.offer(e);
    }

    @Override
    public synchronized E poll() {
        return delegate.poll();
    }

    @Override
    public synchronized E peek() {
        return delegate.peek();
    }

    @Override
    public synchronized int size() {
        return delegate.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized E peekLast(){
        return delegate.peekLast();
    }

    //Not used for reservoir
    @Override
    public synchronized boolean add(E e) {
        return delegate.add(e);
    }

    @Override
    public synchronized E remove() {
        return delegate.remove();
    }

    @Override
    public synchronized E element() {
        return delegate.element();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public synchronized Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }
}
