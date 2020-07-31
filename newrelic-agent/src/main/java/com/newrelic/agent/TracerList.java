/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.tracers.Tracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * This class appears as a list of Tracers. The contents of the list are lazily evaluated from the constructor arguments
 * and then cached.
 */
public class TracerList implements List<Tracer> {

    private final Set<TransactionActivity> activities;
    private List<Tracer> tracers;
    private final Tracer txRootTracer; // root tracer of owning transaction

    public TracerList(Tracer txRootTracer, Set<TransactionActivity> activities) {
        if (activities == null) {
            throw new IllegalArgumentException();
        }
        this.activities = activities;
        this.txRootTracer = txRootTracer;
    }

    // Thread safety: we are fine to reference the TransactionActivities from whatever thread
    // we're currently on because (1) the activity must have finished, (2) been added to the
    // synchronized collection of finished activities, (3) been copied out to be used here.
    private List<Tracer> getTracers() {
        if (tracers == null) {
            int n = 0;
            for (TransactionActivity txa : activities) {
                n += txa.getTracers().size();
            }
            n++; // for the root tracer, in case we add it
            tracers = new ArrayList<>(n);
            for (TransactionActivity txa : activities) {
                if (txa.getRootTracer() != txRootTracer) {
                    tracers.add(txa.getRootTracer());
                }
                tracers.addAll(txa.getTracers());
            }
        }
        return tracers;
    }

    @Override
    public int size() {
        return getTracers().size();
    }

    @Override
    public boolean isEmpty() {
        return getTracers().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return getTracers().contains(o);
    }

    @Override
    public Iterator<Tracer> iterator() {
        return getTracers().iterator();
    }

    @Override
    public Object[] toArray() {
        return getTracers().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getTracers().toArray(a);
    }

    @Override
    public boolean add(Tracer e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        return getTracers().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getTracers().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Tracer> c) {
        return getTracers().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Tracer> c) {
        return getTracers().addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return getTracers().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return getTracers().retainAll(c);
    }

    @Override
    public void clear() {
        getTracers().clear();
    }

    @Override
    public Tracer get(int index) {
        return getTracers().get(index);
    }

    @Override
    public Tracer set(int index, Tracer element) {
        return getTracers().set(index, element);
    }

    @Override
    public void add(int index, Tracer element) {
        getTracers().add(index, element);
    }

    @Override
    public Tracer remove(int index) {
        return getTracers().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return getTracers().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return getTracers().lastIndexOf(o);
    }

    @Override
    public ListIterator<Tracer> listIterator() {
        return getTracers().listIterator();
    }

    @Override
    public ListIterator<Tracer> listIterator(int index) {
        return getTracers().listIterator(index);
    }

    @Override
    public List<Tracer> subList(int fromIndex, int toIndex) {
        return getTracers().subList(fromIndex, toIndex);
    }

}
