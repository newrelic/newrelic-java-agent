package com.newrelic.agent.jmx;

import com.newrelic.api.agent.Logger;
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

    @Test
    public void testRegisterHappyPath() throws Exception {
        ObjectName name = new ObjectName(MBEAN_NAME);
        Logger logger = mock(Logger.class);
        final MBeanServer fakeServer = mock(MBeanServer.class);

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
        ObjectName name = new ObjectName(MBEAN_NAME);
        RuntimeException exception = new RuntimeException("Bad beans exception");

        Logger logger = mock(Logger.class);
        final MBeanServer fakeServer = mock(MBeanServer.class);
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