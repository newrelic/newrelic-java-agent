package com.nr.instrumentation.kotlin.coroutines.test;

import com.newrelic.agent.introspec.TraceSegment;

import java.util.List;
import java.util.Map;

public class TestUtils {

    public static TraceSegment getRequestedTraceSegment(TraceSegment traceSegment, String name) {
        if (traceSegment == null) return null;

        String traceSegmentName = traceSegment.getName();
        if (traceSegmentName.equals(name)) {
            return traceSegment;
        }

        List<TraceSegment> children = traceSegment.getChildren();
        for (TraceSegment child : children) {
            TraceSegment childTraceSegment = getRequestedTraceSegment(child, name);
            if (childTraceSegment != null) {
                return childTraceSegment;
            }
        }

        return null;
    }

    public static TraceSegment getRequestedTraceSegmentStartsWith(TraceSegment traceSegment, String prefix) {
        if (traceSegment == null) return null;

        String traceSegmentName = traceSegment.getName();
        if (traceSegmentName.startsWith(prefix)) {
            return traceSegment;
        }

        List<TraceSegment> children = traceSegment.getChildren();
        for (TraceSegment child : children) {
            TraceSegment childTraceSegment = getRequestedTraceSegmentStartsWith(child, prefix);
            if (childTraceSegment != null) {
                return childTraceSegment;
            }
        }

        return null;
    }

    public static void printTraceSegment(TraceSegment traceSegment, int indents) {
        if (traceSegment == null) return;

        String traceSegmentName = traceSegment.getName();
        for(int i=0; i<indents; i++) {
            System.out.print("\t");
        }
        int callCount = traceSegment.getCallCount();
        Map<String, Object> attributes = traceSegment.getTracerAttributes();
        Object threadId = attributes.get("thread.id");
        System.out.println(traceSegmentName + " " + traceSegmentName + ", call count " + callCount + ", thread id " + threadId);
        List<TraceSegment> children = traceSegment.getChildren();
        for (TraceSegment child : children) {
            printTraceSegment(child, indents + 1);
        }
    }
}
