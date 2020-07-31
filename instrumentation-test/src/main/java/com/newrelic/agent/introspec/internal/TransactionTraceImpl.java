/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class TransactionTraceImpl implements com.newrelic.agent.introspec.TransactionTrace {

    private long startTime;
    private float responseTimeSec;
    private float wallClockDurationSec;
    private Map<String, Object> intrinsics;
    /*
     * I have pulled off the funny root segment that has the wallClockDuration as the endTime and ROOT as the name. I do
     * not think implementors need to know about this funny node.
     */
    private TraceSegment rootSegment;

    private TransactionTraceImpl() {
        super();
    }

    public static TransactionTraceImpl createTransactionTrace(TransactionTrace trace) throws Exception {
        TransactionSegment funnyRootNode = trace.getRootSegment();
        Collection<TransactionSegment> segments = funnyRootNode.getChildren();
        // Take off the funny root node that is not shown in the UI. The name is ROOT and the end time is the total
        // transaction wall clock time. This node should always only have one child which is the real root node.
        if (segments.size() != 1) {
            throw new Exception(
                    "The transaction segment has an invalid format. The ROOT node should only have one child.");
        }
        TraceSegmentImpl rootSeg = TraceSegmentImpl.createTraceSegment(trace, segments.iterator().next());

        TransactionTraceImpl output = new TransactionTraceImpl();
        output.startTime = trace.getStartTime();
        // the duration on the trace is ms - converting to sec
        output.responseTimeSec = (trace.getDuration() / 1000.0f);
        output.rootSegment = rootSeg;
        // end time in ms - converting to sec
        output.wallClockDurationSec = (funnyRootNode.getEndTime() / 1000.0f);
        output.intrinsics = Maps.newHashMap(trace.getIntrinsicsShallowCopy());

        return output;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public float getResponseTimeInSec() {
        return responseTimeSec;
    }

    @Override
    public float getWallClockDurationInSec() {
        return wallClockDurationSec;
    }

    @Override
    public TraceSegment getInitialTraceSegment() {
        return rootSegment;
    }

    @Override
    public Map<String, Object> getIntrinsicAttributes() {
        return Collections.unmodifiableMap(intrinsics);
    }

}
