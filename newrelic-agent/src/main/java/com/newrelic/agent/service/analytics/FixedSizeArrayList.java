/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceUtils;

/**
 * A simple array that accepts elements up to a bound and then ignores all following adds. <br>
 * Does not throw {@link ConcurrentModificationException}. Attempts to be threadsafe by referencing a volatile on reads
 * and writes.
 */
public class FixedSizeArrayList<E> implements List<E> {
    private final Object[] data;
    private final AtomicInteger volatileMemoryBarrier = new AtomicInteger(0);
    protected final int size;
    protected final AtomicInteger numberOfTries = new AtomicInteger();

    public FixedSizeArrayList(int size) {
        this.data = new Object[size];
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) {
        rangeCheck(index);
        ServiceUtils.readMemoryBarrier(volatileMemoryBarrier);
        return (E) data[index];
    }

    @Override
    public boolean add(E t) {
        Integer slot = getSlot();
        if (slot == null) {
            return false;
        }
        set(slot, t);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            modified |= add(e);
        }
        return modified;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E set(int slot, E element) {
        rangeCheck(slot);
        ServiceUtils.readMemoryBarrier(volatileMemoryBarrier);
        E oldValue = (E) data[slot];
        data[slot] = element;
        ServiceUtils.writeMemoryBarrier(volatileMemoryBarrier);
        return oldValue;
    }

    private void rangeCheck(int index) {
        if (index >= data.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.length);
        }
    }

    public Integer getSlot() {
        int insertIndex = numberOfTries.getAndIncrement();
        if (insertIndex >= data.length) {
            return null;
        }
        return insertIndex;
    }

    public int getNumberOfTries() {
        return numberOfTries.get();
    }

    @Override
    public int size() {
        return Math.min(data.length, numberOfTries.get());
    }

    @Override
    public boolean isEmpty() {
        return numberOfTries.get() == 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int cursor; // index of next element to return

            @Override
            public boolean hasNext() {
                return cursor != size();
            }

            @SuppressWarnings("unchecked")
            @Override
            public E next() {
                int i = cursor;
                if (i >= size()) {
                    throw new NoSuchElementException();
                }
                cursor = i + 1;
                ServiceUtils.readMemoryBarrier(volatileMemoryBarrier);
                return (E) data[i];
            }

            @Override
            public void remove() {
                Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
            }
        };
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(data, size());
    }

    @Override
    public boolean contains(Object o) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return false;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return null;
    }

    @Override
    public boolean remove(Object o) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return false;
    }

    @Override
    public void clear() {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
    }

    @Override
    public void add(int index, E element) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
    }

    @Override
    public E remove(int index) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return null;
    }

    @Override
    public int indexOf(Object o) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return null;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return null;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        Agent.LOG.log(Level.FINE, new UnsupportedOperationException(), "Method not implemented.");
        return null;
    }
}
