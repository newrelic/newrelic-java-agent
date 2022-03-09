package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class ApplicationLoggingMetricsConfigTest {

    private Map<String, Object> localProps;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
    }

    @Test
    public void defaultLocalDecoratingConfig() {
        ApplicationLoggingMetricsConfig config = new ApplicationLoggingMetricsConfig(localProps,
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);
        assertTrue(config.getEnabled());

    }

    @Test
    public void usesEnvVarForLocalDecoratingConfig() {

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(
                        Collections.singletonMap("NEW_RELIC_APPLICATION_LOGGING_METRICS_ENABLED", "false"))
        ));

        ApplicationLoggingMetricsConfig config = new ApplicationLoggingMetricsConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);
        assertFalse(config.getEnabled());

    }

    @Test
    public void usesSysPropForLocalDecoratingConfig() {
        Properties properties = new Properties();

        properties.put("newrelic.config.application_logging.metrics.enabled", "" + "false");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        ApplicationLoggingMetricsConfig config = new ApplicationLoggingMetricsConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);
        assertFalse(config.getEnabled());

    }
}