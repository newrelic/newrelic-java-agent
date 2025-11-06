/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.activemq.command;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.activemqclient580.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnection;

import static com.newrelic.agent.bridge.messaging.JmsProperties.NR_JMS_BROKER_INSTANCE_PROPERTY;

@Weave(type = MatchType.BaseClass, originalName = "org.apache.activemq.command.ActiveMQMessage")
public abstract class ActiveMQMessage_Instrumentation {
    public abstract ActiveMQConnection getConnection();

    // This is so the JMS instrumentation can grab host and port of the Active MQ instance
    public Object getObjectProperty(String name) {
        if (NR_JMS_BROKER_INSTANCE_PROPERTY.equals(name)) {
            return ActiveMQUtil.get().parseHostAndPort(getConnection().getTransport().toString());
        }
        return Weaver.callOriginal();
    }
}