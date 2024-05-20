package com.nr.agent.instrumentation.activemqclient580;

import com.newrelic.agent.bridge.messaging.HostAndPort;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.transport.Transport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.jms.JMSException;

import static com.newrelic.agent.bridge.messaging.JmsProperties.NR_JMS_HOST_AND_PORT_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache" })
public class ActiveMQMessageTest {

    private Transport transport;
    private ActiveMQMessage activeMQMessage;

    @Before
    public void setUp() {
        ActiveMQConnection connection = Mockito.mock(ActiveMQConnection.class);
        transport = Mockito.mock(Transport.class);
        Mockito.when(connection.getTransport()).thenReturn(transport);
        activeMQMessage = new ActiveMQMessage();
        activeMQMessage.setConnection(connection);
    }

    @Test
    public void fromMessageGetUnsetProperty() throws Exception {
        Object object = activeMQMessage.getObjectProperty("unsetProperty");
        assertFalse("Object must not be an instance of HostAndPort", object instanceof HostAndPort);
        assertNull("object must be null", object);
    }

    @Test
    public void fromMessageGetHostAndPort() throws Exception {
        final String awsAddress = "ssl://b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com/174.65.25.235:61617";

        final String expectedHost = "b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com";
        final Integer expectedPort = 61617;

        setStubs(awsAddress);

        assertMessage(expectedHost, expectedPort, activeMQMessage);
        // Verify Caching
        assertMessage(expectedHost, expectedPort, activeMQMessage);
    }

    @Test
    public void fromMessageGetHostAndPortWithLocalPort() throws Exception {
        final String awsAddress = "ssl://b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com/174.65.25.235:61617@59925";

        final String expectedHost = "b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com";
        final Integer expectedPort = 61617;

        setStubs(awsAddress);

        assertMessage(expectedHost, expectedPort, activeMQMessage);
        // Verify Caching
        assertMessage(expectedHost, expectedPort, activeMQMessage);
    }

    @Test
    public void fromMessageGetHostAndPortWithLocalHost() throws Exception {
        final String localhostAddress = "tcp://localhost/127.0.0.1:61616@59925";
        final String expectedHost = "localhost";
        final Integer expectedPort = 61616;

        setStubs(localhostAddress);

        assertMessage(expectedHost, expectedPort, activeMQMessage);
        // Verify Caching
        assertMessage(expectedHost, expectedPort, activeMQMessage);
    }

    private void setStubs(String transportString) {
        Mockito.when(transport.toString()).thenReturn(transportString);
    }

    private void assertMessage(String expectedHost, Integer expectedPort, ActiveMQMessage message) throws JMSException {
        HostAndPort hostAndPort = (HostAndPort)message.getObjectProperty(NR_JMS_HOST_AND_PORT_PROPERTY);
        assertNotNull("Failed to retrieve hostAndPort from ActiveMQ message", hostAndPort);
        assertEquals("Expected host did not match", expectedHost, hostAndPort.getHostName());
        assertEquals("Expected port did not match", expectedPort, hostAndPort.getPort());
    }

}
