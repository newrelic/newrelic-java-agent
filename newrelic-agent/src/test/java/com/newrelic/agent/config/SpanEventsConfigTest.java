package com.newrelic.agent.config;

import com.newrelic.agent.Mocks;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.config.SpanEventsConfig.*;
import static org.junit.Assert.*;

public class SpanEventsConfigTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabledByDefault() {
        //given
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(true);
        serverSettings.put(COLLECT_SPAN_EVENTS, serverProp);
        //when
        SpanEventsConfig config = new SpanEventsConfig(serverSettings, true);
        //then
        assertTrue("SpanEventsConfig isEnabled should be true", config.isEnabled());
    }

    @Test
    public void maxSamplesStoredShouldBeDefault() {
        //given
        Map<String, Object> localSettings = new HashMap<>();
        //when
        SpanEventsConfig config = new SpanEventsConfig(localSettings, true);
        //then
        assertEquals("Max samples stored should be " + DEFAULT_MAX_SPAN_EVENTS_PER_HARVEST,
                DEFAULT_MAX_SPAN_EVENTS_PER_HARVEST, config.getMaxSamplesStored());
    }

    @Test
    public void maxSamplesStoredOverrideBySystemProperty() {
        //given
        int customMaxSamples = 1000;
        setMaxSamplesViaSystemProp(customMaxSamples);
        Map<String, Object> localSettings = new HashMap<>();
        //when
        SpanEventsConfig config = new SpanEventsConfig(localSettings, true);
        //then
        assertEquals("Max samples stored should be " + customMaxSamples,
                customMaxSamples, config.getMaxSamplesStored());
    }

    @Test
    public void maxSamplesStoredOverrideByEnvironmentProperty() {
        //given
        int customMaxSamples = 3000;
        setMaxSamplesViaEnvProperty(customMaxSamples);
        Map<String, Object> localSettings = new HashMap<>();
        //when
        SpanEventsConfig config = new SpanEventsConfig(localSettings, true);
        //then
        assertEquals("Max samples stored should be " + customMaxSamples,
                customMaxSamples, config.getMaxSamplesStored());
    }

    @Test
    public void maxSamplesStoredCanBeReset() {
        //given
        Map<String, Object> localSettings = new HashMap<>();
        //when
        SpanEventsConfig config = new SpanEventsConfig(localSettings, true);
        config.setMaxSamplesStoredByServerProp(10000);
        assertEquals("Max samples stored should be " + 10000,
                10000, config.getMaxSamplesStored());
    }

    private void setMaxSamplesViaSystemProp(int customMaxSamples) {
        Map<String, String> properties = new HashMap<>();
        String key = SYSTEM_PROPERTY_ROOT + MAX_SPAN_EVENTS_PER_HARVEST;
        String val = String.valueOf(customMaxSamples);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
    }

    private void setMaxSamplesViaEnvProperty(int customMaxSamples) {
        Map<String, String> sysProperties = new HashMap<>();
        Map<String, String> envProperties = new HashMap<>();
        String key = SYSTEM_PROPERTY_ROOT + MAX_SPAN_EVENTS_PER_HARVEST;
        String val = String.valueOf(customMaxSamples);
        envProperties.put(key, val);
        SystemPropertyProvider testProvider = Mocks.createSystemPropertyProvider(sysProperties,envProperties);
    }


}
