/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer.internals;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.ClientType;
import com.nr.instrumentation.kafka.NewRelicMetricsReporter;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.metrics.Metrics;

import java.util.List;

@Weave(originalName = "org.apache.kafka.clients.consumer.internals.ClassicKafkaConsumer")
public class ClassicKafkaConsumer_Instrumentation<K, V> {
    private final Metrics metrics = Weaver.callOriginal();

    private final ConsumerMetadata metadata = Weaver.callOriginal();

    @NewField
    private boolean initialized;

    @WeaveAllConstructors
    public ClassicKafkaConsumer_Instrumentation() {
        if (!initialized) {
            List<Node> nodes = metadata.fetch().nodes();
            metrics.addReporter(new NewRelicMetricsReporter(ClientType.CONSUMER, nodes));
            initialized = true;
        }
    }
}
