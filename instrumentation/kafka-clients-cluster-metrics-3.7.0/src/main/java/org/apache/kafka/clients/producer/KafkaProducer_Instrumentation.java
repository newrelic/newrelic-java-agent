/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.producer;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.ClusterIdHelper;
import com.nr.instrumentation.kafka.Utils;
import java.util.concurrent.Future;

@Weave(originalName = "org.apache.kafka.clients.producer.KafkaProducer")
public class KafkaProducer_Instrumentation<K, V> {

    @NewField
    private volatile String nrClusterId;

    private Future<RecordMetadata> doSend(ProducerRecord record, Callback callback) {
        if (nrClusterId == null) {
            String id = ClusterIdHelper.fromProducer(this);
            if (id != null) {
                nrClusterId = id;
            }
        }

        final Future<RecordMetadata> result = Weaver.callOriginal();

        if (nrClusterId != null) {
            NewRelic.getAgent().getMetricAggregator().recordMetric(
                    Utils.KAFKA_CLUSTER_METRIC_PREFIX + nrClusterId
                            + Utils.KAFKA_CLUSTER_TOPIC_SEGMENT + record.topic()
                            + Utils.KAFKA_CLUSTER_PRODUCE_SUFFIX,
                    1.0f);
        }

        return result;
    }
}
