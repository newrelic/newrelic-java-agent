package org.apache.activemq.command;

import com.newrelic.agent.bridge.messaging.HostAndPort;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.activemqclient580.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnection;

import javax.jms.JMSException;

import static com.newrelic.agent.bridge.messaging.JmsUtil.NR_JMS_HOST_AND_PORT_PROPERTY;

@Weave(type = MatchType.BaseClass, originalName = "org.apache.activemq.command.ActiveMQMessage")
public abstract class ActiveMQMessage_Instrumentation {
    public abstract ActiveMQConnection getConnection();

    // This is so the JMS instrumentation can grab host and port of the Active MQ instance
    public HostAndPort getObjectProperty(String name) throws JMSException {
        if (NR_JMS_HOST_AND_PORT_PROPERTY.equals(name)) {
            return ActiveMQUtil.parseHostAndPort(getConnection().getTransport().toString());
        }
        return Weaver.callOriginal();
    }
}
