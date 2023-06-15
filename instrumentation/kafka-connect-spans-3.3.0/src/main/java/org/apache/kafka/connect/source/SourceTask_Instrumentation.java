/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.source;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.List;

import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.KAFKA_CONNECT;

@Weave(originalName = "org.apache.kafka.connect.source.SourceTask", type = MatchType.BaseClass)
public abstract class SourceTask_Instrumentation<K, V> {

    @Trace
    public abstract List<SourceRecord> poll() throws InterruptedException;
}