package com.newrelic.agent.jmx;

import com.newrelic.api.agent.Logger;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.logging.Level;

import static com.newrelic.agent.jmx.LinkingMetadataRegistration.MBEAN_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LinkingMetadataRegistrationTest {

    ObjectName name;
    Logger logger;
    MBeanServer fakeServer;

    @Before
    public void setup() throws Exception {
        name = new ObjectName(MBEAN_NAME);
        logger = mock(Logger.class);
        fakeServer = mock(MBeanServer.class);
    }

    @Test
    public void testRegisterHappyPath() throws Exception {

        LinkingMetadataRegistration testClass = new LinkingMetadataRegistration(logger) {
            @Override
            protected MBeanServer getMbeanServer() {
                return fakeServer;
            }
        };

        testClass.registerLinkingMetadata();
        verify(fakeServer).registerMBean(isA(LinkingMetadataMBean.class), eq(name));
    }

    @Test
    public void testExceptionsAreHandled() throws Exception {
        RuntimeException exception = new RuntimeException("Bad beans exception");

        when(fakeServer.registerMBean(isA(LinkingMetadataMBean.class), eq(name))).thenThrow(exception);

        LinkingMetadataRegistration testClass = new LinkingMetadataRegistration(logger) {
            @Override
            protected MBeanServer getMbeanServer() {
                return fakeServer;
            }
        };

        testClass.registerLinkingMetadata();
        //success
        verify(logger).log(eq(Level.INFO), eq("JMX LinkingMetadata started, registering MBean: com.newrelic.jfr:type=LinkingMetadata"));
        verify(logger).log(eq(Level.INFO), eq("Error registering JMX LinkingMetadata MBean"), eq(exception));
    }

    @Test
    public void testNoClassDefErrorAlsoHandled() throws Exception {
        NoClassDefFoundError exception = new NoClassDefFoundError("I am broken.");

        when(fakeServer.registerMBean(isA(LinkingMetadataMBean.class), eq(name))).thenThrow(exception);

        LinkingMetadataRegistration testClass = new LinkingMetadataRegistration(logger) {
            @Override
            protected MBeanServer getMbeanServer() {
                return fakeServer;
            }
        };

        testClass.registerLinkingMetadata();
        //success
        verify(logger).log(eq(Level.INFO), eq("JMX LinkingMetadata started, registering MBean: com.newrelic.jfr:type=LinkingMetadata"));
        verify(logger).log(eq(Level.INFO), eq("Error registering JMX LinkingMetadata MBean"), eq(exception));
    }

}