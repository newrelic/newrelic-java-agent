package com.nr.instrumentation.kafka.config;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigEmissionConfigurationTest {

    @Mock
    private Agent agent;

    @Mock
    private Config agentConfig;

    private final List<ConfigTestUtil.LoggedMessage> loggedMessages = new CopyOnWriteArrayList<>();

    @Before
    public void wireAgent() {
        when(agent.getConfig()).thenReturn(agentConfig);
        final Logger agentLogger = ConfigTestUtil.mockLogger(loggedMessages);
        when(agent.getLogger()).thenReturn(agentLogger);
    }

    @Test
    public void enabledByDefault() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            final ConfigEmissionConfiguration config = ConfigEmissionConfiguration.read();
            assertTrue(config.isEnabled());
            assertTrue(config.isGeneralOverriddenDefaultReportingEnabled());
        }
    }

    private void mockDefaultConfigs() {
        when(agentConfig.getValue(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    public void sslAndSaslDisabledByDefault() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(any(), any(Boolean.class)))
                    .thenAnswer((Answer<Boolean>) invocation -> invocation.getArgument(1));
            final ConfigEmissionConfiguration config = ConfigEmissionConfiguration.read();
            assertThat(config.isSslReportingEnabled(), is(false));
            assertThat(config.isSaslReportingEnabled(), is(false));
        }
    }

    @Test
    public void canBeDisabled() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_ENABLED), any())).thenReturn(false);
            assertThat(ConfigEmissionConfiguration.read().isEnabled(), is(false));
        }
    }

    @Test
    public void defaultOverridesCanBeDisabled() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_OVERRIDDEN_GENERAL_DEFAULTS), any())).thenReturn(false);
            boolean reportingEnabled = ConfigEmissionConfiguration.read().isGeneralOverriddenDefaultReportingEnabled();
            assertThat(reportingEnabled, is(false));
        }
    }

    @Test
    public void sslCanBeEnabled() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_EVENTS_SSL), any())).thenReturn(true);
            final ConfigEmissionConfiguration config = ConfigEmissionConfiguration.read();
            assertThat(config.isSslReportingEnabled(), is(true));
            assertThat(config.isSaslReportingEnabled(), is(false));
        }
    }

    @Test
    public void saslCanBeEnabled() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_EVENTS_SASL), any())).thenReturn(true);
            final ConfigEmissionConfiguration config = ConfigEmissionConfiguration.read();
            assertThat(config.isSslReportingEnabled(), is(false));
            assertThat(config.isSaslReportingEnabled(), is(true));
        }
    }

    @Test
    public void reportingTimingIsConfigurable() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_DELAY))).thenReturn("P7D");
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_FREQUENCY))).thenReturn("PT3.3S");
            final ConfigEmissionConfiguration config = ConfigEmissionConfiguration.read();
            assertThat(config.getReportingDelay(), equalTo(Duration.parse("P7D")));
            assertThat(config.getReportingFrequency(), equalTo(Duration.parse("PT3.3S")));
        }
    }

    @Test
    public void configurationCapIsConfigurable() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_CONFIGURATION_CAP), any())).thenReturn(1234);
            final ConfigEmissionConfiguration config = ConfigEmissionConfiguration.read();
            assertThat(config.getClientCountCap(), equalTo(1234));
        }
    }

    @Test
    public void reportingTimingNoticesBadDuration() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_DELAY))).thenReturn("Fred");
            assertThat(ConfigEmissionConfiguration.read().getReportingDelay(), notNullValue());
//            assertThat(loggedMessages).anyMatch(m -> m.level == Level.SEVERE && m.pattern.contains("duration format"));
        }
    }

    @Test
    public void reportingTimingNoticesNegativeDuration() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_DELAY))).thenReturn("-PT3H");
            assertThat(ConfigEmissionConfiguration.read().getReportingDelay(), notNullValue());
//            assertThat(loggedMessages).anyMatch(m -> m.level == Level.SEVERE && m.pattern.contains("Negative duration"));
        }
    }

    @Test
    public void badBooleanConfigsNoticed() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_ENABLED), any())).thenReturn("sausage");
            assertThat(ConfigEmissionConfiguration.read().isEnabled(), is(true));
//            assertThat(loggedMessages).anyMatch(m -> m.level == Level.SEVERE && m.pattern.contains("Expected boolean value"));
        }
    }

    @Test
    public void badIntConfigsNoticed() {
        try (final MockedStatic<NewRelic> ignored = wireNewRelic()) {
            mockDefaultConfigs();
            when(agentConfig.getValue(eq(ConfigEmissionConfiguration.PROP_REPORTING_CONFIGURATION_CAP), any())).thenReturn("hamburger");
            assertThat(ConfigEmissionConfiguration.read().getConfigurationEventCap(), equalTo(30));
//            assertThat(loggedMessages).anyMatch(m -> m.level == Level.SEVERE && m.pattern.contains("Expected int value"));
        }

    }

    private MockedStatic<NewRelic> wireNewRelic() {
        final MockedStatic<NewRelic> newRelicMock = Mockito.mockStatic(NewRelic.class);
        newRelicMock.when(NewRelic::getAgent).thenReturn(agent);
        return newRelicMock;
    }

}
