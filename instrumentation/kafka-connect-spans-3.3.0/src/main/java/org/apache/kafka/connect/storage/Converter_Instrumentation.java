/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.storage;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;

import java.util.logging.Level;

import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.KAFKA_CONNECT;

@Weave(originalName = "org.apache.kafka.connect.storage.Converter", type = MatchType.Interface)
public abstract class Converter_Instrumentation {

    @Trace(excludeFromTransactionTrace = true)
    public abstract byte[] fromConnectData(String topic, Schema schema, Object value);

    @Trace(excludeFromTransactionTrace = true)
    public abstract SchemaAndValue toConnectData(String topic, byte[] value);
}
