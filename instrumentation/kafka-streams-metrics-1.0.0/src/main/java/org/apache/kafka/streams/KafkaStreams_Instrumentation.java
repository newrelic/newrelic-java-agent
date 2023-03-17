/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.streams;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.NewRelicMetricsReporter;
import org.apache.kafka.common.metrics.Metrics;

@Weave(originalName = "org.apache.kafka.streams.KafkaStreams")
public class KafkaStreams_Instrumentation {
    private final Metrics metrics = Weaver.callOriginal();

    @NewField
    private boolean nrMetricsInitialized;

    @WeaveAllConstructors
    public KafkaStreams_Instrumentation() {
        if (!nrMetricsInitialized) {
            metrics.addReporter(new NewRelicMetricsReporter());
            nrMetricsInitialized = true;
        }
    }
}
