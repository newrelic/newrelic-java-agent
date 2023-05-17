/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class tracks thread sampling data for an execution point. It records the number of times an execution point was
 * hit, and the called execution points.
 */
public class ProfileSegment implements JSONStreamAware {

    private final ProfiledMethod method;
    private int runnableCallCount = 0;
    private int nonrunnableCallCount = 0;
    private final Map<ProfiledMethod, ProfileSegment> children = new IdentityHashMap<>();

    /**
     * Parameters are guaranteed to be non-null.
     * 
     * @param method the execution point
     */
    private ProfileSegment(ProfiledMethod method) {
        this.method = method;
    }

    /**
     * Create a new ProfileSegment from a {@link ProfiledMethod}
     * 
     * @param method the execution point
     */
    public static ProfileSegment newProfileSegment(ProfiledMethod method) {
        if (method == null) {
            return null;
        }

        return new ProfileSegment(method);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        try {
            JSONArray.writeJSONString(Arrays.asList(method, runnableCallCount, nonrunnableCallCount,
                    new ArrayList<>(children.values())), out);
        } catch (StackOverflowError e) {
        }
    }

    @Override
    public String toString() {
        return method.toString();
    }

    public ProfiledMethod getMethod() {
        return method;
    }

    /**
     * Returns the number of times this segment has appeared in the profile thread samples.
     * 
     */
    protected int getRunnableCallCount() {
        return runnableCallCount;
    }

    public void incrementRunnableCallCount() {
        runnableCallCount++;
    }

    public void incrementNonRunnableCallCount() {
        nonrunnableCallCount++;
    }

    Collection<ProfileSegment> getChildren() {
        return children.values();
    }

    Map<ProfiledMethod, ProfileSegment> getChildMap() {
        return children;
    }

    /**
     * Add a child segment to this segment if it doesn't already exist.
     * 
     * @param method the {@link ProfiledMethod} for the child
     * @return the {@link ProfileSegment} for the child
     */
    ProfileSegment addChild(ProfiledMethod method) {
        ProfileSegment result = children.get(method);
        if (result == null) {
            result = ProfileSegment.newProfileSegment(method);
            children.put(method, result);
        }
        return result;
    }

    /**
     * Remove a child segment from this segment.
     * 
     * @param method the {@link ProfiledMethod} for the child
     */
    void removeChild(ProfiledMethod method) {
        children.remove(method);
    }

    /**
     * Get the number of profile segments from this segment down
     * 
     * @return the size of the tree with this segment as root
     */
    public int getCallSiteCount() {
        int count = 1;

        for (ProfileSegment segment : children.values()) {
            count += segment.getCallSiteCount();
        }

        return count;
    }

    public int getCallCount(ProfiledMethod method) {
        int count = method.equals(getMethod()) ? this.runnableCallCount : 0;
        for (ProfileSegment kid : children.values()) {
            count += kid.getCallCount(method);
        }

        return count;
    }

    public Set<ProfiledMethod> getMethods() {
        Set<ProfiledMethod> methods = new HashSet<>();
        methods.add(getMethod());
        for (ProfileSegment kid : children.values()) {
            methods.addAll(kid.getMethods());
        }
        return methods;
    }
}
