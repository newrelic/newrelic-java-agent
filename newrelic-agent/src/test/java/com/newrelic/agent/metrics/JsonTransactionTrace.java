/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class JsonTransactionTrace {

    private final String testName;
    private final String fileName;
    private long duration;
    private String scope;
    private JsonTraceSegment rootSegment;
    private Map<String, Object> intrinsics;

    private JsonTransactionTrace(String pTestName, String pFileName) {
        testName = pTestName;
        fileName = pFileName;
    }

    public static JsonTransactionTrace createJsonTransactionTrace(JSONObject ttObject, String pTestname,
            String pFileName) {

        JsonTransactionTrace trace = new JsonTransactionTrace(pTestname, pFileName);
        Object obj = ttObject.get("duration");
        trace.duration = (obj == null) ? null : (Long) obj;
        obj = ttObject.get("scope");
        trace.scope = (obj == null) ? null : (String) obj;

        obj = ttObject.get("segments");
        trace.rootSegment = (obj == null) ? null : JsonTraceSegment.createTraceSegment((JSONArray) obj, pTestname,
                pFileName);

        trace.intrinsics = new HashMap<>();
        obj = ttObject.get("intrinsics");
        if (obj != null) {
            JSONObject data = (JSONObject) obj;
            for (Object curr : data.entrySet()) {
                Entry current = (Entry) curr;
                trace.intrinsics.put((String) current.getKey(), current.getValue());
            }
        }
        return trace;
    }

    public void validateTransactionTrace(TransactionTrace actualTrace) {
        String msg = fileName + " \"" + testName + "\"" + " Invalid ";
        Assert.assertEquals(msg + "duration", duration, actualTrace.getDuration());
        Assert.assertEquals(msg + "scope", scope, actualTrace.getRootMetricName());
        TransactionSegment actualRoot = actualTrace.getRootSegment();
        rootSegment.validateTraceSegment(actualRoot);

        // System.out.println(DataSenderWriter.toJSONString(actualTrace));

        Map<String, Object> actualInt = actualTrace.getIntrinsicsShallowCopy();
        Assert.assertEquals(intrinsics.size(), actualInt.size());
        for (Entry<String, Object> curr : intrinsics.entrySet()) {
            Object value = actualInt.get(curr.getKey());
            Assert.assertNotNull("value for " + curr.getKey() + " should not be null", value);
            // do not check value if it is a star
            if (!"*".equals(curr.getValue())) {
                if (curr.getValue() instanceof Number) {
                    Assert.assertEquals(((Number) curr.getValue()).floatValue(), ((Number) value).floatValue(), .001);
                } else if (curr.getValue() instanceof String) {
                    Assert.assertEquals(curr.getValue().toString(), value.toString());
                } else {
                    Assert.assertEquals(curr.getValue(), value);
                }
            }
        }

    }
}
