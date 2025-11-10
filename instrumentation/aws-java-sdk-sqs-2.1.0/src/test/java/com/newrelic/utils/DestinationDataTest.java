/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class DestinationDataTest {

    @Test
    public void parse() {
        DestinationData data = DestinationData.parse("https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue");

        assertNotNull(data);
        assertEquals("us-east-2", data.getRegion());
        assertEquals("123456789012", data.getAccountId());
        assertEquals("MyQueue", data.getQueueName());
    }

    @Test
    public void parse_bogus() {
        DestinationData data = DestinationData.parse("bogus");

        assertNotNull(data);
        assertNull(data.getRegion());
        assertNull(data.getAccountId());
        assertEquals("unknown", data.getQueueName());
    }

    @Test
    public void parse_nonMatchingUrl() {
        DestinationData data = DestinationData.parse("https://not-sqs.us-east-2.amazonaws.com/123456789012/MyQueue");

        assertNotNull(data);
        assertNull(data.getRegion());
        assertNull(data.getAccountId());
        assertEquals("MyQueue", data.getQueueName());
    }

}