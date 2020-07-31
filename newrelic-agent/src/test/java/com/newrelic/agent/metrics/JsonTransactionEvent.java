/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONObject;
import org.junit.Assert;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.TransactionStats;

public class JsonTransactionEvent {

    private String fileName;
    private String testName;
    private Long legacyDuration;
    private Long totalTime;
    private Long firstByte;
    private Long lastByte;

    public static JsonTransactionEvent createTransactionEvent(JSONObject eventObj, String pTestName, String pFileName) {

        JsonTransactionEvent seg = new JsonTransactionEvent();
        seg.fileName = pFileName;
        seg.testName = pTestName;

        Object obj = eventObj.get("duration");
        seg.legacyDuration = (obj == null) ? null : (Long) obj;

        obj = eventObj.get("total_time");
        seg.totalTime = (obj == null) ? null : (Long) obj;

        obj = eventObj.get("ttfb");
        seg.firstByte = (obj == null) ? null : (Long) obj;

        obj = eventObj.get("ttlb");
        seg.lastByte = (obj == null) ? null : (Long) obj;

        return seg;
    }

    public void verifyTransactionEvent(TransactionData data, TransactionStats stats) {
        TransactionEvent actual = ServiceFactory.getTransactionEventsService().createEvent(data, stats, data.getBlameMetricName());
        // System.out.println(DataSenderWriter.toJSONString(actual));
        String msg = fileName + " \"" + testName + "\" Invalid ";
        if (legacyDuration != null) {
            Assert.assertEquals(msg + "duration", legacyDuration, actual.getDuration() * 1000, .01);
        }
        if (totalTime != null) {
            Assert.assertEquals(msg + "total time", totalTime, actual.getTotalTime() * 1000, .01);
        }
        if (firstByte != null) {
            Assert.assertEquals(msg + "ttfb", firstByte, actual.getTTFB() * 1000, .01);
        }
        if (lastByte != null) {
            Assert.assertEquals(msg + "ttlb", lastByte, actual.getTTLB() * 1000, .01);
        }
    }
}
