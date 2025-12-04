package com.newrelic.agent.jfr;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JfrConfig;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.daemon.DaemonConfig;
import com.newrelic.jfr.daemon.JfrRecorderException;
import com.newrelic.test.marker.IBMJ9IncompatibleTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.newrelic.agent.config.AgentConfigImpl.DEFAULT_EVENT_INGEST_URI;
import static com.newrelic.agent.config.AgentConfigImpl.DEFAULT_METRIC_INGEST_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JfrServiceTest {

    @Mock
    JfrConfig jfrConfig;

    @Mock
    AgentConfig agentConfig;

    @Mock
    ServerlessConfig serverlessConfig;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        when(jfrConfig.useLicenseKey()).thenReturn(true);
        when(jfrConfig.getHarvestInterval()).thenReturn(22);
        when(jfrConfig.getQueueSize()).thenReturn(300_000);
        when(agentConfig.getApplicationName()).thenReturn("test_app_name");
        when(agentConfig.getMetricIngestUri()).thenReturn(DEFAULT_METRIC_INGEST_URI);
        when(agentConfig.getEventIngestUri()).thenReturn(DEFAULT_EVENT_INGEST_URI);
        when(agentConfig.getLicenseKey()).thenReturn("test_1234_license_key");
        when(agentConfig.getProxyScheme()).thenReturn("http");
        when(agentConfig.getValue(eq(ThreadService.NAME_PATTERN_CFG_KEY), any(String.class)))
                .thenReturn(ThreadNameNormalizer.DEFAULT_PATTERN);
        when(agentConfig.getServerlessConfig()).thenReturn(serverlessConfig);
        when(serverlessConfig.isEnabled()).thenReturn(false);
    }

    @Test
    public void daemonConfigBuiltCorrect() {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        DaemonConfig daemonConfig = jfrService.buildDaemonConfig();

        assertTrue(daemonConfig.useLicenseKey());
        assertEquals("test_1234_license_key", daemonConfig.getApiKey());
        assertEquals("test_app_name", daemonConfig.getMonitoredAppName());
        assertEquals(DEFAULT_METRIC_INGEST_URI, daemonConfig.getMetricsUri().toString());
        assertEquals(DEFAULT_EVENT_INGEST_URI, daemonConfig.getEventsUri().toString());
        assertEquals(22, daemonConfig.getHarvestInterval().getSeconds());
        assertEquals(300_000, (int)daemonConfig.getQueueSize());
        assertEquals("http", daemonConfig.getProxyScheme());
    }

    @Test
    public void isEnabledIsCorrect() {
        when(jfrConfig.isEnabled()).thenReturn(true);
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        assertTrue(jfrService.isEnabled());
    }

    @Test
    public void jfrLoopDoesNotStartWhenCoreApiIsFalse() throws JfrRecorderException {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);
        when(spyJfr.coreApisExist()).thenReturn(false);
        when(spyJfr.isEnabled()).thenReturn(true);

        spyJfr.doStart();

        assertFalse(spyJfr.coreApisExist());
        verify(spyJfr, times(0)).startJfrLoop();
    }

    @Test
    public void jfrLoopDoesNotStartWhenIsEnabledIsFalse() throws JfrRecorderException {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);
        when(spyJfr.coreApisExist()).thenReturn(true);
        when(spyJfr.isEnabled()).thenReturn(false);

        spyJfr.doStart();

        assertFalse(spyJfr.isEnabled());
        verify(spyJfr, times(0)).startJfrLoop();
    }

    @Test
    public void jfrLoopDoesNotStartWhenIsEnabledIsTrueAndHighSecurityIsTrue() throws JfrRecorderException {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);
        when(agentConfig.isHighSecurity()).thenReturn(true);
        when(jfrConfig.isEnabled()).thenReturn(true);
        when(spyJfr.coreApisExist()).thenReturn(true);

        spyJfr.doStart();

        assertFalse(spyJfr.isEnabled());
        verify(spyJfr, times(0)).startJfrLoop();
    }

    @Test
    public void jfrLoopDoesNotStartWhenIsEnabledIsTrueAndServerlessModeIsTrue() throws JfrRecorderException {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);
        when(serverlessConfig.isEnabled()).thenReturn(true);
        when(jfrConfig.isEnabled()).thenReturn(true);
        when(spyJfr.coreApisExist()).thenReturn(true);

        spyJfr.doStart();

        assertFalse(spyJfr.isEnabled());
        verify(spyJfr, times(0)).startJfrLoop();
    }

    @Category( IBMJ9IncompatibleTest.class )
    @Test
    public void jfrLoopDoesStart() {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);
        when(spyJfr.coreApisExist()).thenReturn(true);
        when(spyJfr.isEnabled()).thenReturn(true);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);
        RPMServiceManager mockRPMServiceManager = manager.getRPMServiceManager();
        RPMService mockRPMService = mock(RPMService.class);
        when(mockRPMServiceManager.getRPMService()).thenReturn(mockRPMService);
        when(mockRPMService.getEntityGuid()).thenReturn("test_guid");

        spyJfr.doStart();

        try {
            //The timeout wait is necessary because jfr loop is being executed on async thread.
            verify(spyJfr, timeout(100)).startJfrLoop();
            spyJfr.doStop();
        } catch (JfrRecorderException e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void configChanged_RespectsChangedSetting() {
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);
        AgentConfig newAgentConfig = mock(AgentConfig.class);
        JfrConfig newJfrConfig = mock(JfrConfig.class);

        when(jfrConfig.isEnabled()).thenReturn(true);
        when(newAgentConfig.getJfrConfig()).thenReturn(newJfrConfig);
        when(newJfrConfig.isEnabled()).thenReturn(false);

        spyJfr.configChanged("my-app", newAgentConfig);
        verify(spyJfr).doStop();
    }
}