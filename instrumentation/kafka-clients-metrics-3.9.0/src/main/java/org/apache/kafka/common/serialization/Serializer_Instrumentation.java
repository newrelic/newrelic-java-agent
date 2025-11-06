/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.common.serialization;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.Metrics;

@Weave(originalName = "org.apache.kafka.common.serialization.Serializer", type = MatchType.Interface)
public class Serializer_Instrumentation<T> {

    public byte[] serialize(String topic, T data) {
        long start = System.nanoTime();
        byte[] result = Weaver.callOriginal();
        NewRelic.recordMetric(Metrics.SERIALIZATION_TIME_METRIC_BASE + topic, System.nanoTime() - start);
        return result;
    }
}
