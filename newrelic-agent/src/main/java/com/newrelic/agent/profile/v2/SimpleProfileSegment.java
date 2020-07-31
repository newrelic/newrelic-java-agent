/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import org.json.simple.JSONArray;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

public class SimpleProfileSegment extends ProfileSegment {
   
    private int runnableCallCount = 0;
    private int nonrunnableCallCount = 0;

    /**
     * Parameters are guaranteed to be non-null.
     * 
     * @param method the execution point
     */
    private SimpleProfileSegment(ProfiledMethod method) {
        super(method);
    }

    /**
     * Create a new ProfileSegment from a {@link ProfiledMethod}
     * 
     * @param method the execution point
     */
    public static SimpleProfileSegment newProfileSegment(ProfiledMethod method) {
        if (method == null) {
            return null;
        }

        return new SimpleProfileSegment(method);
    }
    
    @Override
    protected SimpleProfileSegment createProfileSegment(ProfiledMethod method) {
        return SimpleProfileSegment.newProfileSegment(method);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(method, runnableCallCount, nonrunnableCallCount,
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
        return runnableCallCount;
    }

    @Override
    public void incrementCallCount(boolean runnable) {
        if (runnable) {
            runnableCallCount++;
        } else {
            nonrunnableCallCount++;
        }
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
        int count = method.equals(getMethod()) ? this.runnableCallCount : 0;
        for (ProfileSegment kid : children.values()) {
            count += kid.getCallCount(method);
        }

        return count;
    }

}
