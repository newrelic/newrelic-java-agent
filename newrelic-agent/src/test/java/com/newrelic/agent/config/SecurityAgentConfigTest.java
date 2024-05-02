package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_AGENT_ENABLED;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_AGENT_ENABLED_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_DETECTION_DESERIALIZATION_ENABLED;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_DETECTION_DESERIALIZATION_ENABLED_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_DETECTION_RCI_ENABLED;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_DETECTION_RCI_ENABLED_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_DETECTION_RXSS_ENABLED;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_DETECTION_RXSS_ENABLED_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_ENABLED;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_ENABLED_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_MODE;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_MODE_DEFAULT;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_VALIDATOR_SERVICE_URL;
import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_VALIDATOR_SERVICE_URL_DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityAgentConfigTest {
    private static MockedStatic<NewRelic> mockNewRelic;
    private static Agent mockAgent;
    private static com.newrelic.api.agent.Config mockConfig;

    @BeforeClass
    public static void beforeTestSuite() {
        mockNewRelic = mockStatic(NewRelic.class);

        mockAgent = mock(Agent.class);
        mockConfig = mock(com.newrelic.api.agent.Config.class);
        mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
        when(mockAgent.getConfig()).thenReturn(mockConfig);
    }

    @AfterClass
    public static void afterTestSuite() {
        mockNewRelic.close();
    }

    @Test
    public void addSecurityAgentConfigSupportabilityMetrics_incrementsMetric() {
        when(mockConfig.getValue(SECURITY_ENABLED, SecurityAgentConfig.SECURITY_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);

        SecurityAgentConfig.addSecurityAgentConfigSupportabilityMetrics();

        mockNewRelic.verify(() -> NewRelic.incrementCounter("Supportability/Java/SecurityAgent/Enabled/enabled"));
        mockNewRelic.verify(() -> NewRelic.incrementCounter("Supportability/Java/SecurityAgent/Agent/Enabled/enabled"));
    }

    @Test
    public void shouldInitializeSecurityAgent_returnsCorrectInitValue() {
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(SECURITY_ENABLED)).thenReturn(true);
        assertTrue(SecurityAgentConfig.shouldInitializeSecurityAgent());

        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(true);
        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(SECURITY_ENABLED)).thenReturn(true);
        assertFalse(SecurityAgentConfig.shouldInitializeSecurityAgent());

        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(false);
        when(mockConfig.getValue(SECURITY_ENABLED)).thenReturn(true);
        assertFalse(SecurityAgentConfig.shouldInitializeSecurityAgent());

        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(SECURITY_ENABLED)).thenReturn(null);
        assertFalse(SecurityAgentConfig.shouldInitializeSecurityAgent());
    }

    @Test
    public void isSecurityAgentEnabled_returnsCorrectEnabledValue() {
        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        assertTrue(SecurityAgentConfig.isSecurityAgentEnabled());

        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(false);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        assertFalse(SecurityAgentConfig.isSecurityAgentEnabled());

        when(mockConfig.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(true);
        assertFalse(SecurityAgentConfig.isSecurityAgentEnabled());
    }

    @Test
    public void isSecurityEnabled_returnsCorrectEnabledFlag() {
        when(mockConfig.getValue(SECURITY_ENABLED, SECURITY_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        assertTrue(SecurityAgentConfig.isSecurityEnabled());

        when(mockConfig.getValue(SECURITY_ENABLED, SECURITY_ENABLED_DEFAULT)).thenReturn(false);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(false);
        assertFalse(SecurityAgentConfig.isSecurityEnabled());

        when(mockConfig.getValue(SECURITY_ENABLED, SECURITY_ENABLED_DEFAULT)).thenReturn(true);
        when(mockConfig.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY)).thenReturn(true);
        assertFalse(SecurityAgentConfig.isSecurityEnabled());
    }

    @Test
    public void isSecurityDetectionRciEnabled_returnsCorrectDetectionFlag() {
        when(mockConfig.getValue(SECURITY_DETECTION_RCI_ENABLED, SECURITY_DETECTION_RCI_ENABLED_DEFAULT)).thenReturn(true);
        assertTrue(SecurityAgentConfig.isSecurityDetectionRciEnabled());

        when(mockConfig.getValue(SECURITY_DETECTION_RCI_ENABLED, SECURITY_DETECTION_RCI_ENABLED_DEFAULT)).thenReturn(false);
        assertFalse(SecurityAgentConfig.isSecurityDetectionRciEnabled());
    }

    @Test
    public void isSecurityDetectionRxssEnabled_returnsCorrectDetectionFlag() {
        when(mockConfig.getValue(SECURITY_DETECTION_RXSS_ENABLED, SECURITY_DETECTION_RXSS_ENABLED_DEFAULT)).thenReturn(true);
        assertTrue(SecurityAgentConfig.isSecurityDetectionRxssEnabled());

        when(mockConfig.getValue(SECURITY_DETECTION_RXSS_ENABLED, SECURITY_DETECTION_RXSS_ENABLED_DEFAULT)).thenReturn(false);
        assertFalse(SecurityAgentConfig.isSecurityDetectionRxssEnabled());
    }

    @Test
    public void isSecurityDetectionDeserializationEnabled_returnsCorrectDetectionFlag() {
        when(mockConfig.getValue(SECURITY_DETECTION_DESERIALIZATION_ENABLED, SECURITY_DETECTION_DESERIALIZATION_ENABLED_DEFAULT)).thenReturn(true);
        assertTrue(SecurityAgentConfig.isSecurityDetectionDeserializationEnabled());

        when(mockConfig.getValue(SECURITY_DETECTION_DESERIALIZATION_ENABLED, SECURITY_DETECTION_DESERIALIZATION_ENABLED_DEFAULT)).thenReturn(false);
        assertFalse(SecurityAgentConfig.isSecurityDetectionDeserializationEnabled());
    }

    @Test
    public void getSecurityAgentValidatorServiceUrl_returnsCorrectUrl() {
        when(mockConfig.getValue(SECURITY_VALIDATOR_SERVICE_URL, SECURITY_VALIDATOR_SERVICE_URL_DEFAULT)).thenReturn("url");
        assertEquals("url", SecurityAgentConfig.getSecurityAgentValidatorServiceUrl());
    }

    @Test
    public void getSecurityAgentMode_returnsCorrectMode() {
        when(mockConfig.getValue(SECURITY_MODE, SECURITY_MODE_DEFAULT)).thenReturn("mode");
        assertEquals("mode", SecurityAgentConfig.getSecurityAgentMode());
    }

    @Test
    public void isSecurityLowPriorityInstrumentationEnabled_returnsCorrectEnabledFlag() {
        when(mockConfig.getValue(SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED, SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED_DEFAULT)).thenReturn(true);
        assertTrue(SecurityAgentConfig.isSecurityLowPriorityInstrumentationEnabled());

        when(mockConfig.getValue(SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED, SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED_DEFAULT)).thenReturn(false);
        assertFalse(SecurityAgentConfig.isSecurityLowPriorityInstrumentationEnabled());
    }

    @Test
    public void testLogSettings() {
        Config config = mock(Config.class);
        Logger logger = mock(Logger.class);
        when(logger.isLoggable(Level.FINE)).thenReturn(true);
        final String test = "test";
        final String environmentValueToSkip = "SECURITY_THING";
        Map<String, String> env = ImmutableMap.of("NEW_RELIC_SECURITY_AGENT_ENABLED", "true",
                environmentValueToSkip, test);
        final String systemPropertyToSkip = "security.test";
        Map<Object, Object> systemProperties = ImmutableMap.of("newrelic.config.security.enabled", "true",
                systemPropertyToSkip, test);
        SecurityAgentConfig.logSettings(config, logger, Level.FINE, env, systemProperties);

        verify(logger, times(1)).log(Level.FINE, "Environment {0} = {1}", "NEW_RELIC_SECURITY_AGENT_ENABLED", "true");
        verify(logger, times(0)).log(Level.FINE, "Environment {0} = {1}", environmentValueToSkip, test);
        verify(logger, times(1)).log(Level.FINE, "System property {0} = {1}", "newrelic.config.security.enabled", "true");
        verify(logger, times(0)).log(Level.FINE, "System property {0} = {1}", systemPropertyToSkip, test);
    }
}
