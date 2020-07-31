/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.newrelic.agent.tracers.Tracer;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TraceProfileSegment extends ProfileSegment {
   
    private final AtomicInteger runnableCallCount = new AtomicInteger();
    private final AtomicLong totalDurationInNanos = new AtomicLong();
    private final AtomicLong exclusiveDurationInNanos = new AtomicLong();

    /**
     * Parameters are guaranteed to be non-null.
     * 
     * @param method the execution point
     */
    private TraceProfileSegment(ProfiledMethod method) {
        super(method);
    }

    /**
     * Create a new ProfileSegment from a {@link ProfiledMethod}
     * 
     * @param method the execution point
     */
    public static TraceProfileSegment newProfileSegment(ProfiledMethod method) {
        if (method == null) {
            return null;
        }

        return new TraceProfileSegment(method);
    }

    @Override
    protected TraceProfileSegment createProfileSegment(ProfiledMethod method) {
        return TraceProfileSegment.newProfileSegment(method);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(method, 
                runnableCallCount.get(),
                TimeUnit.NANOSECONDS.toMillis(totalDurationInNanos.get()),
                TimeUnit.NANOSECONDS.toMillis(exclusiveDurationInNanos.get()),
                new ArrayList<>(children.values())), out);
    }

    @Override
    public String toString() {
        return method.toString();
    }

    /**
     * Returns the number of times this segment has appeared in the profile thread samples.
     * 
     */
    @Override
    protected int getRunnableCallCount() {
        return runnableCallCount.get();
    }

    @Override
    public void incrementCallCount(boolean runnable) {
        runnableCallCount.incrementAndGet();
    }

    /**
     * Get the number of profile segments from this segment down
     * 
     * @return the size of the tree with this segment as root
     */
    @Override
    public int getCallSiteCount() {
        int count = 1;

        for (ProfileSegment segment : children.values()) {
            count += segment.getCallSiteCount();
        }

        return count;
    }

    @Override
    public int getCallCount(ProfiledMethod method) {
        int count = method.equals(getMethod()) ? this.runnableCallCount.get() : 0;
        for (ProfileSegment kid : children.values()) {
            count += kid.getCallCount(method);
        }

        return count;
    }

    public void update(Tracer tracer) {
        totalDurationInNanos.getAndAdd(tracer.getDuration());
        exclusiveDurationInNanos.getAndAdd(tracer.getExclusiveDuration());
    }

}
