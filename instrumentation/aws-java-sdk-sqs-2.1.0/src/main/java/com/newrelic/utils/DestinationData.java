/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that holds the data, retrieved from the queue URL,
 * used in the span attributes to identify the destination.
 */
class DestinationData {
    private final String region;
    private final String accountId;
    private final String queueName;

    private static final DestinationData UNKNOWN = new DestinationData(null, null, "unknown");
    private static final Pattern QUEUE_URL_PATTERN = Pattern.compile(
            "https://sqs.(?<region>[^.]+).amazonaws.com/(?<accountId>[^/]+)/(?<queueName>.*)");
    private static final Map<String, DestinationData> CACHE = AgentBridge.collectionFactory
            .createConcurrentTimeBasedEvictionMap(3600 * 8); // 8 hours

    private DestinationData(String region, String accountId, String queueName) {
        this.region = region;
        this.accountId = accountId;
        this.queueName = queueName;
    }

    public String getRegion() {
        return region;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getQueueName() {
        return queueName;
    }

    public static DestinationData parse(String queueUrl) {
        return CACHE.computeIfAbsent(queueUrl, DestinationData::doParse);
    }

    private static DestinationData doParse(String queueUrl) {
        Matcher matcher = QUEUE_URL_PATTERN.matcher(queueUrl);
        if (matcher.matches()) {
            return new DestinationData(
                    matcher.group("region"),
                    matcher.group("accountId"),
                    matcher.group("queueName")
            );
        } else {
            int index = queueUrl.lastIndexOf('/');
            if (index <= 0) {
                return UNKNOWN;
            }
            String queueName = queueUrl.substring(index + 1);
            return new DestinationData(null, null, queueName);
        }
    }
}
