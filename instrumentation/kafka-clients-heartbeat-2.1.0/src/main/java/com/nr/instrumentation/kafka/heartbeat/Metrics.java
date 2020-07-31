/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.heartbeat;

public class Metrics {

    // Heartbeat metrics
    public static final String HEARTBEAT_POLL = "MessageBroker/Kafka/Heartbeat/Poll";
    public static final String HEARTBEAT_SENT = "MessageBroker/Kafka/Heartbeat/Sent";
    public static final String HEARTBEAT_FAIL = "MessageBroker/Kafka/Heartbeat/Fail";
    public static final String HEARTBEAT_RECEIVE = "MessageBroker/Kafka/Heartbeat/Receive";
    public static final String HEARTBEAT_SESSION_TIMEOUT = "MessageBroker/Kafka/Heartbeat/SessionTimeout";
    public static final String HEARTBEAT_POLL_TIMEOUT = "MessageBroker/Kafka/Heartbeat/PollTimeout";

}
