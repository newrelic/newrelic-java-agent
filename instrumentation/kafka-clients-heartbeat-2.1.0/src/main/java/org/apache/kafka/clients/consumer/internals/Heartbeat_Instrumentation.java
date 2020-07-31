/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import static com.nr.instrumentation.kafka.heartbeat.Metrics.HEARTBEAT_FAIL;
import static com.nr.instrumentation.kafka.heartbeat.Metrics.HEARTBEAT_POLL;
import static com.nr.instrumentation.kafka.heartbeat.Metrics.HEARTBEAT_POLL_TIMEOUT;
import static com.nr.instrumentation.kafka.heartbeat.Metrics.HEARTBEAT_RECEIVE;
import static com.nr.instrumentation.kafka.heartbeat.Metrics.HEARTBEAT_SENT;
import static com.nr.instrumentation.kafka.heartbeat.Metrics.HEARTBEAT_SESSION_TIMEOUT;

@Weave(originalName = "org.apache.kafka.clients.consumer.internals.Heartbeat")
public final class Heartbeat_Instrumentation {

    public void poll(long now) {
        Weaver.callOriginal();
        NewRelic.incrementCounter(HEARTBEAT_POLL);
    }

    public void sentHeartbeat(long now) {
        Weaver.callOriginal();
        NewRelic.incrementCounter(HEARTBEAT_SENT);
    }

    public void failHeartbeat() {
        Weaver.callOriginal();
        NewRelic.incrementCounter(HEARTBEAT_FAIL);
    }

    public void receiveHeartbeat() {
        Weaver.callOriginal();
        NewRelic.incrementCounter(HEARTBEAT_RECEIVE);
    }

    public boolean sessionTimeoutExpired(long now) {
        boolean result = Weaver.callOriginal();
        if (result) {
            NewRelic.incrementCounter(HEARTBEAT_SESSION_TIMEOUT);
        }
        return result;
    }

    public boolean pollTimeoutExpired(long now) {
        boolean result = Weaver.callOriginal();
        if (result) {
            NewRelic.incrementCounter(HEARTBEAT_POLL_TIMEOUT);
        }
        return result;
    }

}
