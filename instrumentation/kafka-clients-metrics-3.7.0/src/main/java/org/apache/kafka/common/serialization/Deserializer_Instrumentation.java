/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.common.serialization;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.Metrics;

@Weave(originalName = "org.apache.kafka.common.serialization.Deserializer", type = MatchType.Interface)
public class Deserializer_Instrumentation<T> {

    public T deserialize(String topic, byte[] data) {
        long start = System.nanoTime();
        T result = Weaver.callOriginal();
        NewRelic.recordMetric(Metrics.DESERIALIZATION_TIME_METRIC_BASE + topic, System.nanoTime() - start);
        return result;
    }
}
