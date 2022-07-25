/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This class is here to prevent this module from applying to Kafka 3.
 */
@Weave(originalName = "org.apache.kafka.clients.consumer.ConsumerRecord")
public class ConsumerRecord_Instrumentation {

    public long checksum() {
        return Weaver.callOriginal();
    }
}
