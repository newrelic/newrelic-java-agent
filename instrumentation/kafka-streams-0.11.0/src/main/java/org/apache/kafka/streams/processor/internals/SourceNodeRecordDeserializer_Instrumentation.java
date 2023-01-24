/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.processor.ProcessorContext;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.SourceNodeRecordDeserializer")
class SourceNodeRecordDeserializer_Instrumentation {

    public ConsumerRecord<Object, Object> deserialize(final ConsumerRecord<byte[], byte[]> rawRecord) {
        ConsumerRecord<Object, Object> result = Weaver.callOriginal();

        // Copy headers
        if (result != null) {
            for (Header header : rawRecord.headers()) {
                result.headers().add(header);
            }
        }

        return result;
    }
}
