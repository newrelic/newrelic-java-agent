/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;

class TraceSegmentImpl implements TraceSegment {

    private String name;
    private long startMs;
    private long endMs;
    private String className;
    private String methodName;
    private String uri;
    private int callCount;

    private Map<String, Object> atts;
    List<TraceSegment> children;

    private TraceSegmentImpl() {
        super();
    }

    static TraceSegmentImpl createTraceSegment(TransactionTrace trace, TransactionSegment tracer) {
        TraceSegmentImpl output = new TraceSegmentImpl();
        output.name = tracer.getMetricName();
        output.startMs = tracer.getStartTime();
        output.endMs = tracer.getEndTime();
        output.className = tracer.getClassName();
        output.methodName = tracer.getMethodName();
        output.atts = Maps.newHashMap(tracer.getTraceParameters());
        output.atts.putAll(trace.getUserAttributes());
        Collection<TransactionSegment> originalChildren = tracer.getChildren();
        output.children = new ArrayList<>(originalChildren.size());
        for (TransactionSegment current : originalChildren) {
            TraceSegmentImpl impl = createTraceSegment(trace, current);
            output.children.add(impl);
        }
        output.uri = tracer.getUri();
        output.callCount = tracer.getCallCount();
        return output;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getRelativeStartTime() {
        return startMs;
    }

    @Override
    public long getRelativeEndTime() {
        return endMs;
    }

    @Override
    public Map<String, Object> getTracerAttributes() {
        return atts;
    }

    @Override
    public List<TraceSegment> getChildren() {
        return children;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public int getCallCount() {
        return callCount;
    }
}
