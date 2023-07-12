/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.runtime;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.util.ConnectorTaskId;

import java.util.List;

import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.MESSAGE;
import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.KAFKA_CONNECT;

@Weave(originalName = "org.apache.kafka.connect.runtime.WorkerSourceTask")
abstract class WorkerSourceTask_Instrumentation {

    @NewField
    private Token token;

    @Trace(dispatcher = true)
    protected List<SourceRecord> poll() throws InterruptedException {
        NewRelic.getAgent().getTransaction()
                .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, MESSAGE, KAFKA_CONNECT, id().connector());
        List<SourceRecord> returnValue = Weaver.callOriginal();
        if (returnValue == null || returnValue.isEmpty()) {
            NewRelic.getAgent().getTransaction().ignore();
        } else {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
        return returnValue;
    }

    @Trace(async = true)
    private boolean sendRecords() {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    public abstract ConnectorTaskId id();
}
