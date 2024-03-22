package com.newrelic.agent.jmx;

import com.newrelic.agent.bridge.JmxApi;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.values.EmbeddedTomcatDataSourceJmxValues;
import com.newrelic.agent.jmx.values.EmbeddedTomcatJmxValues;
import com.newrelic.agent.jmx.values.GlassfishJmxValues;
import com.newrelic.agent.jmx.values.Jboss7UpJmxValues;
import com.newrelic.agent.jmx.values.JettyJmxMetrics;
import com.newrelic.agent.jmx.values.KafkaConsumerJmxValues;
import com.newrelic.agent.jmx.values.KafkaProducerJmxValues;
import com.newrelic.agent.jmx.values.LegacySolr7JmxValues;
import com.newrelic.agent.jmx.values.ResinJmxValues;
import com.newrelic.agent.jmx.values.Solr7JmxValues;
import com.newrelic.agent.jmx.values.SolrJmxValues;
import com.newrelic.agent.jmx.values.TomcatJmxValues;
import com.newrelic.agent.jmx.values.WebSphere7JmxValues;
import com.newrelic.agent.jmx.values.WebSphereJmxValues;
import com.newrelic.agent.jmx.values.WeblogicJmxValues;
import com.newrelic.agent.jmx.values.WebsphereLibertyJmxValues;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JmxApiImplTest {

    private JmxApi jmxApi;

    @Before
    public void setUp() throws Exception {
        jmxApi = new JmxApiImpl();
    }

    @Test
    public void addJmxMBeanGroup() {
        try (MockedStatic<ServiceFactory> serviceFactory = mockStatic(ServiceFactory.class)) {
            JmxService jmxService = mock(JmxService.class);
            serviceFactory.when(ServiceFactory::getJmxService)
                    .thenReturn(jmxService);

            jmxApi.addJmxMBeanGroup("kafka.producer");

            verify(jmxService).addJmxFrameworkValues(any(KafkaProducerJmxValues.class));

            // adding a second time, to verify it is no added again
            jmxApi.addJmxMBeanGroup("kafka.producer");

            verifyNoMoreInteractions(jmxService);
        }
    }

    @Test
    public void addJmxMBeanGroup_addAll_when_iteratedObjectNames_Enabled() {
        Map<String, Class<? extends JmxFrameworkValues>> map = new HashMap<>();
        map.put(KafkaProducerJmxValues.PREFIX, KafkaProducerJmxValues.class);
        map.put(KafkaConsumerJmxValues.PREFIX, KafkaConsumerJmxValues.class);
        map.put(WebSphere7JmxValues.PREFIX, WebSphere7JmxValues.class);
        map.put(WebSphereJmxValues.PREFIX, WebSphereJmxValues.class);
        map.put(SolrJmxValues.PREFIX, SolrJmxValues.class);
        map.put(Solr7JmxValues.PREFIX, Solr7JmxValues.class);
        map.put(WebsphereLibertyJmxValues.PREFIX, WebsphereLibertyJmxValues.class);
        map.put(TomcatJmxValues.PREFIX, TomcatJmxValues.class);
        map.put(EmbeddedTomcatJmxValues.PREFIX, EmbeddedTomcatJmxValues.class);
        map.put(EmbeddedTomcatDataSourceJmxValues.PREFIX, EmbeddedTomcatDataSourceJmxValues.class);
        map.put(JettyJmxMetrics.PREFIX, JettyJmxMetrics.class);
        map.put(Jboss7UpJmxValues.PREFIX, Jboss7UpJmxValues.class);
        map.put(ResinJmxValues.PREFIX, ResinJmxValues.class);
        map.put(GlassfishJmxValues.PREFIX, GlassfishJmxValues.class);
        map.put(WeblogicJmxValues.PREFIX, WeblogicJmxValues.class);

        try (MockedStatic<ServiceFactory> serviceFactory = mockStatic(ServiceFactory.class)) {
            JmxService jmxService = mock(JmxService.class);
            serviceFactory.when(ServiceFactory::getJmxService)
                    .thenReturn(jmxService);
            when(jmxService.iteratedObjectNameKeysEnabled())
                    .thenReturn(true);

            for (String prefix : map.keySet()) {
                jmxApi.addJmxMBeanGroup(prefix);
            }

            verify(jmxService).iteratedObjectNameKeysEnabled();
            for (Class<? extends JmxFrameworkValues> jmxValues : map.values()) {
                verify(jmxService).addJmxFrameworkValues(any(jmxValues));
            }
            verify(jmxService, never()).addJmxFrameworkValues(any(LegacySolr7JmxValues.class));
            verifyNoMoreInteractions(jmxService);

            assertEquals(Solr7JmxValues.PREFIX, LegacySolr7JmxValues.PREFIX);
        }
    }

    @Test
    public void addJmxMBeanGroup_addAll_when_iteratedObjectNames_Disabled() {
        Map<String, Class<? extends JmxFrameworkValues>> map = new HashMap<>();
        map.put(KafkaProducerJmxValues.PREFIX, KafkaProducerJmxValues.class);
        map.put(KafkaConsumerJmxValues.PREFIX, KafkaConsumerJmxValues.class);
        map.put(WebSphere7JmxValues.PREFIX, WebSphere7JmxValues.class);
        map.put(WebSphereJmxValues.PREFIX, WebSphereJmxValues.class);
        map.put(SolrJmxValues.PREFIX, SolrJmxValues.class);
        map.put(LegacySolr7JmxValues.PREFIX, LegacySolr7JmxValues.class);
        map.put(WebsphereLibertyJmxValues.PREFIX, WebsphereLibertyJmxValues.class);
        map.put(TomcatJmxValues.PREFIX, TomcatJmxValues.class);
        map.put(EmbeddedTomcatJmxValues.PREFIX, EmbeddedTomcatJmxValues.class);
        map.put(EmbeddedTomcatDataSourceJmxValues.PREFIX, EmbeddedTomcatDataSourceJmxValues.class);
        map.put(JettyJmxMetrics.PREFIX, JettyJmxMetrics.class);
        map.put(Jboss7UpJmxValues.PREFIX, Jboss7UpJmxValues.class);
        map.put(ResinJmxValues.PREFIX, ResinJmxValues.class);
        map.put(GlassfishJmxValues.PREFIX, GlassfishJmxValues.class);
        map.put(WeblogicJmxValues.PREFIX, WeblogicJmxValues.class);

        try (MockedStatic<ServiceFactory> serviceFactory = mockStatic(ServiceFactory.class)) {
            JmxService jmxService = mock(JmxService.class);
            serviceFactory.when(ServiceFactory::getJmxService)
                    .thenReturn(jmxService);
            when(jmxService.iteratedObjectNameKeysEnabled())
                    .thenReturn(false);

            for (String prefix : map.keySet()) {
                jmxApi.addJmxMBeanGroup(prefix);
            }

            verify(jmxService).iteratedObjectNameKeysEnabled();
            for (Class<? extends JmxFrameworkValues> jmxValues : map.values()) {
                verify(jmxService).addJmxFrameworkValues(any(jmxValues));
            }
            verify(jmxService, never()).addJmxFrameworkValues(any(Solr7JmxValues.class));
            verifyNoMoreInteractions(jmxService);

            assertEquals(Solr7JmxValues.PREFIX, LegacySolr7JmxValues.PREFIX);
        }
    }

    @Test
    public void addJmxMBeanGroup_frameworkNotFound() {
        try (MockedStatic<ServiceFactory> serviceFactory = mockStatic(ServiceFactory.class)) {
            JmxService jmxService = mock(JmxService.class);
            serviceFactory.when(ServiceFactory::getJmxService)
                    .thenReturn(jmxService);

            jmxApi.addJmxMBeanGroup("name that will not match");

            serviceFactory.verify(ServiceFactory::getJmxService);
            serviceFactory.verifyNoMoreInteractions();
            verifyNoInteractions(jmxService);
        }
    }

    @Test
    public void createMBeanServerIfNeeded() {
        try (MockedStatic<MBeanServerFactory> mBeanServerFactory = mockStatic(MBeanServerFactory.class)) {
            mBeanServerFactory.when(() -> MBeanServerFactory.findMBeanServer(isNull()))
                    .thenReturn(new ArrayList<>());

            jmxApi.createMBeanServerIfNeeded();

            mBeanServerFactory.verify(MBeanServerFactory::createMBeanServer);
        }
    }

    @Test
    public void createMBeanServerIfNeeded_notNeeded() {
        try (MockedStatic<MBeanServerFactory> mBeanServerFactory = mockStatic(MBeanServerFactory.class)) {
            ArrayList<MBeanServer> mBeanServers = new ArrayList<>();
            mBeanServers.add(mock(MBeanServer.class));
            mBeanServerFactory.when(() -> MBeanServerFactory.findMBeanServer(isNull()))
                    .thenReturn(mBeanServers);

            JmxApiImpl jmxApi = new JmxApiImpl();

            jmxApi.createMBeanServerIfNeeded();

            mBeanServerFactory.verify(() -> MBeanServerFactory.findMBeanServer(isNull()));
            mBeanServerFactory.verifyNoMoreInteractions();
        }
    }
}