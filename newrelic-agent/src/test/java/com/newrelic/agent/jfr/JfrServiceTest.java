package com.newrelic.agent.jfr;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JfrConfig;
import com.newrelic.agent.config.LabelsConfig;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.daemon.DaemonConfig;
import com.newrelic.jfr.daemon.JfrRecorderException;
import com.newrelic.telemetry.Attributes;
import com.newrelic.test.marker.Flaky;
import com.newrelic.test.marker.IBMJ9IncompatibleTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.config.AgentConfigImpl.DEFAULT_EVENT_INGEST_URI;
import static com.newrelic.agent.config.AgentConfigImpl.DEFAULT_METRIC_INGEST_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    @Category( { IBMJ9IncompatibleTest.class, Flaky.class } )
    @Test
    // Flaky note: org.mockito.exceptions.verification.WantedButNotInvoked on verify(spyJfr, timeout(100)).startJfrLoop();
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

    @Test
    public void getJfrHostnameOrDisplayName_UseDisplayNameFalse_ReturnsDefaultHostname() {
        when(jfrConfig.useDisplayName()).thenReturn(false);

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);

        when(spyJfr.getHostname()).thenReturn("test-hostname:8080");

        String result = spyJfr.getJfrHostnameOrDisplayName();
        assertEquals("test-hostname:8080", result);
        verify(spyJfr).getHostname();
    }

    @Test
    public void getJfrHostnameOrDisplayName_UseDisplayNameTrue_WithDisplayNameSet_ReturnsDisplayName() {
        when(jfrConfig.useDisplayName()).thenReturn(true);
        when(agentConfig.getValue("process_host.display_name", "test-hostname:8080"))
                .thenReturn("my-custom-display-name");

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);

        when(spyJfr.getHostname()).thenReturn("test-hostname:8080");

        String result = spyJfr.getJfrHostnameOrDisplayName();
        assertEquals("my-custom-display-name", result);
    }

    @Test
    public void getJfrHostnameOrDisplayName_UseDisplayNameTrue_WithDisplayNameEmpty_ReturnsDefaultHostname() {
        when(jfrConfig.useDisplayName()).thenReturn(true);

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);

        String defaultHostname = "test-hostname:8080";
        when(spyJfr.getHostname()).thenReturn(defaultHostname);

        when(agentConfig.getValue("process_host.display_name", defaultHostname))
                .thenReturn(defaultHostname);

        String result = spyJfr.getJfrHostnameOrDisplayName();
        assertEquals(defaultHostname, result);
    }

    @Test
    public void addLabelsEnabledAddsLabelsAsAttrs() {
        /*
         * labels:
         *   team: java-agent
         *   environment: production
         *   region: us-east-1
         *
         * jfr:
         *   labels:
         *     enabled: true
         */
        LabelsConfig labelsConfig = mock(LabelsConfig.class);
        Map<String, String> labels = mockLabelsMap();
        when(labelsConfig.getLabels()).thenReturn(labels);
        when(agentConfig.getLabelsConfig()).thenReturn(labelsConfig);
        when(jfrConfig.labelsEnabled()).thenReturn(true);

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);

        Attributes attrsToModify = new Attributes();
        jfrService.addLabelsIfEnabled(attrsToModify);

        assertEquals(3, attrsToModify.asMap().size());
        assertEquals("java-agent", attrsToModify.asMap().get("tags.team"));
        assertEquals("production", attrsToModify.asMap().get("tags.environment"));
        assertEquals("us-east-1", attrsToModify.asMap().get("tags.region"));
    }

    @Test
    public void addLabelsDisabledDoesNotAddLabelsAsAttrs() {
        /*
         * labels:
         *   team: java-agent
         *   environment: production
         *   region: us-east-1
         *
         * jfr:
         *   labels:
         *     enabled: false
         */
        LabelsConfig labelsConfig = mock(LabelsConfig.class);
        Map<String, String> labels = mockLabelsMap();
        when(labelsConfig.getLabels()).thenReturn(labels);
        when(agentConfig.getLabelsConfig()).thenReturn(labelsConfig);
        when(jfrConfig.labelsEnabled()).thenReturn(false);

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);

        // Test addLabelsIfEnabled
        Attributes attrs = new Attributes();
        jfrService.addLabelsIfEnabled(attrs);

        // Verify no labels are added
        assertEquals(0, attrs.asMap().size());
    }

    @Test
    public void addLabelsEnabledLabelsEmptyIsNoop(){
        /*
        * labels:
        *   //nothing
        *
        * jfr:
        *   labels:
        *     enabled: true
         */

        LabelsConfig labelsConfig = mock(LabelsConfig.class);
        when(labelsConfig.getLabels()).thenReturn(Collections.emptyMap());
        when(agentConfig.getLabelsConfig()).thenReturn(labelsConfig);
        when(jfrConfig.labelsEnabled()).thenReturn(true);

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);

        Attributes attrs = new Attributes();
        jfrService.addLabelsIfEnabled(attrs);

        assertEquals(0, attrs.asMap().size());
    }

    @Test
    public void addLabelsEnabledNullLabelsDoesNotThrow() {
        //The labels map should never be null, but still good to check.
        LabelsConfig labelsConfig = mock(LabelsConfig.class);
        when(labelsConfig.getLabels()).thenReturn(null);
        when(agentConfig.getLabelsConfig()).thenReturn(labelsConfig);
        when(jfrConfig.labelsEnabled()).thenReturn(true);

        JfrService jfrService = new JfrService(jfrConfig, agentConfig);

        Attributes attrs = new Attributes();
        try {
            jfrService.addLabelsIfEnabled(attrs);
            assertEquals(0, attrs.asMap().size());
        } catch (NullPointerException e) {
            fail("Should not throw if labels map is null.");
        }
    }

    @Test
    public void configChangedFromEnabledToDisabledDoesStop() {
        //Initial config: JFR enabled
        when(jfrConfig.isEnabled()).thenReturn(true);
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);

        //Updated config: JFR disabled
        AgentConfig newAgentConfig = mock(AgentConfig.class);
        JfrConfig newJfrConfig = mock(JfrConfig.class);
        when(newAgentConfig.getJfrConfig()).thenReturn(newJfrConfig);
        when(newJfrConfig.isEnabled()).thenReturn(false);

        spyJfr.configChanged("my-app", newAgentConfig);

        verify(spyJfr).doStop();
    }

    @Test
    public void configChangedFromDisabledToEnabledDoesStart() {
        //Initial config: JFR disabled.
        when(jfrConfig.isEnabled()).thenReturn(false);
        JfrService jfrService = new JfrService(jfrConfig, agentConfig);
        JfrService spyJfr = spy(jfrService);

        //Updated config: JFR enabled.
        AgentConfig newAgentConfig = mock(AgentConfig.class);
        JfrConfig newJfrConfig = mock(JfrConfig.class);
        when(newAgentConfig.getJfrConfig()).thenReturn(newJfrConfig);
        when(newJfrConfig.isEnabled()).thenReturn(true);

        spyJfr.configChanged("my-app", newAgentConfig);

        verify(spyJfr).doStart();
    }

    private Map<String, String> mockLabelsMap(){
        Map<String, String> labels = new HashMap<>();
        labels.put("team", "java-agent");
        labels.put("environment", "production");
        labels.put("region", "us-east-1");
        return labels;
    }
}