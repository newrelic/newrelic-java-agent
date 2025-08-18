package com.newrelic.agent.config;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule.TestEnvironmentFacade;
import com.newrelic.agent.SaveSystemPropertyProviderRule.TestSystemProps;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OtelConfigTest {

    private Map<String, Object> configProps;

    private static final String ENV_VAR_METRICS_EXCLUDE = "NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE";

    private static final String SYSTEM_PROP_METRICS_EXCLUDE = "newrelic.config.opentelemetry.metrics.exclude";
    @Before
    public void setup(){
        configProps = new HashMap<>();
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                new TestEnvironmentFacade()
        ));
    }

    @Test
    public void testDefaultConfigValues() {
        OtelConfig config = new OtelConfig(configProps);
        assertTrue(config.getExcludedMeters().isEmpty());
    }

    @Test
    public void testGetExcludedMetersFromProps(){
        configProps.put("metrics.exclude", "foo,bar,baz");
        OtelConfig config = new OtelConfig(configProps);
        assertEquals(3, config.getExcludedMeters().size());
        assertTrue(config.getExcludedMeters().contains("foo"));
        assertTrue(config.getExcludedMeters().contains("bar"));
        assertTrue(config.getExcludedMeters().contains("baz"));
    }

    @Test
    public void testExcludeMetersFromEnvVars(){
        TestEnvironmentFacade environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                ENV_VAR_METRICS_EXCLUDE, "hello,foo-bar,goodbye"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));
        OtelConfig config = new OtelConfig(configProps);
        assertEquals(3, config.getExcludedMeters().size());
        assertTrue(config.getExcludedMeters().contains("hello"));
        assertTrue(config.getExcludedMeters().contains("foo-bar"));
        assertTrue(config.getExcludedMeters().contains("goodbye"));
    }

    @Test
    public void testExcludeMetersFromSystemProps(){
        Properties props = new Properties();
        props.put(SYSTEM_PROP_METRICS_EXCLUDE, "apple,banana");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider( new TestSystemProps(props), new TestEnvironmentFacade()));
        OtelConfig config = new OtelConfig(configProps);
        assertEquals(2, config.getExcludedMeters().size());
        assertTrue(config.getExcludedMeters().contains("apple"));
        assertTrue(config.getExcludedMeters().contains("banana"));
    }


}