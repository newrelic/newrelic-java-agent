/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.transforms;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.connect.connector.ConnectRecord;

import java.util.logging.Level;

import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.KAFKA_CONNECT;

@Weave(originalName = "org.apache.kafka.connect.transforms.Transformation", type = MatchType.Interface)
public abstract class Transformation_Instrumentation<R extends ConnectRecord<R>> {

    @Trace(excludeFromTransactionTrace = true)
    public abstract R apply(R record);
}
