/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds thread sampling data for threads of a given thread grouping.
 */
public class ProfileTree extends BaseTree<SimpleProfileSegment> {

    private long cpuTime;
    private final boolean reportCpuTime;

    public ProfileTree(IProfile profile, boolean reportCpuTime) {
        super(profile);
        this.reportCpuTime = reportCpuTime;
    }

    /**
     * @param stackTraceElement
     * @param parent
     * @param runnable
     * @return a ProfileSegment representing the stack trace element passed in
     */
    private SimpleProfileSegment add(StackTraceElement stackTraceElement, SimpleProfileSegment parent, boolean runnable) {
        ProfiledMethod method = profile.getProfiledMethodFactory().getProfiledMethod(stackTraceElement);

        if (method == null) {
            return parent;
        }

        return add(method, parent, runnable);
    }
    
    public void addStackTrace(
            List<StackTraceElement> stackTraceList, 
            boolean runnable) {
        SimpleProfileSegment parent = null;
        for (StackTraceElement methodCall : stackTraceList) {
            parent = add(methodCall, parent, runnable);
        }
    }

    @Override
    protected Map<String, Object> getExtraData() {
        if (reportCpuTime) {
            Map<String, Object> data = new HashMap<>();
            data.put("cpu_time", cpuTime);
            return data;
        } else {
            return ImmutableMap.of();
        }
    }
    
    // Package-private for testing
    ProfileSegment getSegmentForMethod(StackTraceElement stackTraceElement) {
        ProfiledMethod method = profile.getProfiledMethodFactory().getProfiledMethod(stackTraceElement);

        if (method == null) {
            return null;
        }

        return getSegment(method);
    }

    public void incrementCpuTime(long cpuTime) {
        this.cpuTime += cpuTime;
    }

    public long getCpuTime() {
        return cpuTime;
    }

    @Override
    protected SimpleProfileSegment createProfiledMethod(ProfiledMethod method) {
        return SimpleProfileSegment.newProfileSegment(method);
    }
}
