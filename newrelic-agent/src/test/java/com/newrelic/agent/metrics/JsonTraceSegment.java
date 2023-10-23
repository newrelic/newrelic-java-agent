/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import com.newrelic.agent.trace.TransactionSegment;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JsonTraceSegment {

    private String fileName;
    private String testName;
    private long start;
    private long end;
    private String segName;
    private Map<String, Object> requestParams;
    private List<JsonTraceSegment> children;
    private String className;
    private String methodName;

    public static JsonTraceSegment createTraceSegment(JSONArray segmentArray, String pTestName, String pFileName) {
        if (segmentArray.size() != 7) {
            throw new IllegalArgumentException("A metric array should have 7 positions and not " + segmentArray.size());
        }
        JsonTraceSegment seg = new JsonTraceSegment();
        seg.fileName = pFileName;
        seg.testName = pTestName;
        seg.start = (Long) segmentArray.get(0);
        seg.end = (Long) segmentArray.get(1);
        seg.segName = (String) segmentArray.get(2);
        seg.requestParams = new HashMap<>();
        JSONObject params = (JSONObject) segmentArray.get(3);
        for (Object current : params.entrySet()) {
            Entry<String, Object> input = (Entry<String, Object>) current;
            seg.requestParams.put(input.getKey(), input.getValue());
        }
        // children
        seg.children = new ArrayList<>();
        JSONArray inputChildren = (JSONArray) segmentArray.get(4);
        for (Object current : inputChildren) {
            seg.children.add(createTraceSegment((JSONArray) current, pTestName, pFileName));
        }
        seg.className = (String) segmentArray.get(5);
        seg.methodName = (String) segmentArray.get(6);

        // attributes added to all traces
        seg.requestParams.put("code.namespace", seg.className);
        seg.requestParams.put("code.function", seg.methodName);
        seg.requestParams.put("thread.id", "*");
        return seg;
    }

    public void validateTraceSegment(TransactionSegment actualSegment) {
        // System.out.println(DataSenderWriter.toJSONString(actualSegment));
        String message = fileName + " \"" + testName + "\" " + segName + " Invalid ";
        Assert.assertEquals(message + "startime", start, actualSegment.getStartTime());
        Assert.assertEquals(message + "endtime", end, actualSegment.getEndTime());
        Assert.assertEquals(message + "segname", segName, actualSegment.getMetricName());
        Assert.assertEquals(message + "className", className, actualSegment.getClassName());
        Assert.assertEquals(message + "methodname", methodName, actualSegment.getMethodName());

        Assert.assertEquals(message + "request param size", requestParams.size(),
                actualSegment.getTraceParameters().size());
        for (Entry<String, Object> current : requestParams.entrySet()) {
            Object actual = actualSegment.getTraceParameters().get(current.getKey());
            Assert.assertNotNull(actual);
            // star means we are not validating the actual value
            if (!current.getValue().equals("*")) {
                Assert.assertEquals(fileName + "\"" + testName + "\" Att " + current.getKey()
                        + " has invalid value on segment " + segName, current.getValue(), actual);
            }
        }

        Assert.assertEquals(message + "children size", children.size(), actualSegment.getChildren().size());
        List<TransactionSegment> sortedActual = new ArrayList<>(actualSegment.getChildren());
        Collections.sort(sortedActual, new Comparator<TransactionSegment>() {
            @Override
            public int compare(TransactionSegment first, TransactionSegment second) {
                if (first.getStartTime() == second.getStartTime()) {
                    return String.CASE_INSENSITIVE_ORDER.compare(first.getMetricName(), second.getMetricName());
                } else if (first.getStartTime() > second.getStartTime()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        List<JsonTraceSegment> sortedExpected = new ArrayList<>(children);
        Collections.sort(sortedExpected, new Comparator<JsonTraceSegment>() {
            @Override
            public int compare(JsonTraceSegment first, JsonTraceSegment second) {
                if (first.start == second.start) {
                    return String.CASE_INSENSITIVE_ORDER.compare(first.segName, second.segName);
                } else if (first.start > second.start) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        for (int i = 0; i < sortedExpected.size(); i++) {
            sortedExpected.get(i).validateTraceSegment(sortedActual.get(i));
        }
    }
}
