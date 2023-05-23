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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class holds thread sampling data for threads of a given type.
 * 
 * @see ThreadType
 */
public class ProfileTree implements JSONStreamAware {

    private final Map<ProfiledMethod, ProfileSegment> rootSegments = new IdentityHashMap<>();
    /**
     * A map of stack trace elements to ProfiledMethods. This helps us create fewer {@link ProfiledMethod} instances and
     * allows us to use identity hashmaps.
     */
    private final Map<StackTraceElement, ProfiledMethod> profiledMethods = new HashMap<>();

    private long cpuTime;

    public ProfileTree() {
    }

    /**
     * @param stackTraceElement
     * @param parent
     * @param runnable
     * @return a ProfileSegment representing the stack trace element passed in
     */
    private ProfileSegment add(StackTraceElement stackTraceElement, ProfileSegment parent, boolean runnable) {
        ProfiledMethod method = profiledMethods.get(stackTraceElement);
        if (method == null) {
            method = ProfiledMethod.newProfiledMethod(stackTraceElement);
            if (method != null) {
                profiledMethods.put(stackTraceElement, method);
            }
        }

        if (method == null) {
            return parent;
        }

        return add(method, parent, runnable);
    }

    private ProfileSegment add(ProfiledMethod method, ProfileSegment parent, boolean runnable) {
        ProfileSegment result = add(method, parent);
        if (runnable) {
            result.incrementRunnableCallCount();
        } else {
            result.incrementNonRunnableCallCount();
        }

        return result;
    }

    private ProfileSegment add(ProfiledMethod method, ProfileSegment parent) {
        ProfileSegment result;
        if (parent == null) {
            result = rootSegments.get(method);
            if (result == null) {
                result = ProfileSegment.newProfileSegment(method);
                rootSegments.put(method, result);
            }
        } else {
            result = parent.addChild(method);
        }
        return result;
    }

    /**
     * Return number of calls to given method from all invocations
     * 
     * @param stackElement
     */
    public int getCallCount(StackTraceElement stackElement) {
        ProfiledMethod method = ProfiledMethod.newProfiledMethod(stackElement);
        if (method == null) {
            return 0;
        }

        int count = 0;
        for (ProfileSegment segment : rootSegments.values()) {
            count += segment.getCallCount(method);
        }
        return count;
    }

    /**
     * Returns the number of distinct method invocation nodes in the tree.
     * 
     */
    public int getCallSiteCount() {
        int count = 0;
        for (ProfileSegment segment : rootSegments.values()) {
            count += segment.getCallSiteCount();
        }
        return count;
    }

    public Collection<ProfileSegment> getRootSegments() {
        return rootSegments.values();
    }

    public int getRootCount() {
        return getRootSegments().size();
    }

    public int getMethodCount() {
        Set<ProfiledMethod> methodNames = new HashSet<>();
        for (ProfileSegment segment : rootSegments.values()) {
            methodNames.addAll(segment.getMethods());
        }
        return methodNames.size();
    }

    public void addStackTrace(List<StackTraceElement> stackTraceList, boolean runnable) {
        ProfileSegment parent = null;
        for (StackTraceElement methodCall : stackTraceList) {
            parent = add(methodCall, parent, runnable);
        }
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        Collection<ProfileSegment> rootSegments = getRootSegments();
        ArrayList<Object> list = new ArrayList<>(rootSegments.size() + 1);
        list.add(getExtraData());
        list.addAll(rootSegments);
        JSONArray.writeJSONString(list, out);
    }

    private Map<String, Object> getExtraData() {
        Map<String, Object> data = new HashMap<>();

        data.put("cpu_time", cpuTime);

        return data;
    }

    public void incrementCpuTime(long cpuTime) {
        this.cpuTime += cpuTime;
    }

    public long getCpuTime() {
        return cpuTime;
    }

    public void setMethodDetails(Map<String, Class<?>> classMap) {
        for (ProfiledMethod method : profiledMethods.values()) {
            method.setMethodDetails(classMap);
        }
    }
}
