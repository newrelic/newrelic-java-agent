/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class BaseTree<S extends ProfileSegment> implements JSONStreamAware {

    private final ConcurrentMap<ProfiledMethod, S> rootSegments;

    protected final IProfile profile;

    protected BaseTree(IProfile profile) {
        this.profile = profile;
        this.rootSegments = new ConcurrentHashMap<>();
    }

    protected final S add(ProfiledMethod method, S parent, boolean runnable) {
        S result = add(method, parent);
        result.incrementCallCount(runnable);

        return result;
    }

    private S add(ProfiledMethod method, S parent) {
        S result;
        if (parent == null) {
            result = rootSegments.get(method);
            if (result == null) {
                result = createProfiledMethod(method);
                S previousValue = rootSegments.putIfAbsent(method, result);
                if (null != previousValue) {
                    return previousValue;
                }
            }
        } else {
            result = parent.addChild(method);
        }
        return result;
    }

    protected abstract S createProfiledMethod(ProfiledMethod method);

    /**
     * Returns the number of distinct method invocation nodes in the tree.
     * 
     */
    public final int getCallSiteCount() {
        int count = 0;
        for (S segment : rootSegments.values()) {
            count += segment.getCallSiteCount();
        }
        return count;
    }

    /**
     * Returns the segment that matches the distinct method invocation
     * 
     * @param profiledMethod the method invocation to retrieve
     * @return a segment representing the provided method
     */
    public final S getSegment(ProfiledMethod profiledMethod) {
        return rootSegments.get(profiledMethod);
    }

    public final Collection<S> getRootSegments() {
        return rootSegments.values();
    }

    public final int getRootCount() {
        return getRootSegments().size();
    }

    public final int getMethodCount() {
        Set<ProfiledMethod> methodNames = new HashSet<>();
        for (S segment : rootSegments.values()) {
            methodNames.addAll(segment.getMethods());
        }
        return methodNames.size();
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        Collection<S> rootSegments = getRootSegments();
        ArrayList<Object> list = new ArrayList<>(rootSegments.size() + 1);
        list.add(getExtraData());
        list.addAll(rootSegments);
        JSONArray.writeJSONString(list, out);
    }

    protected abstract Map<String, Object> getExtraData();
}
