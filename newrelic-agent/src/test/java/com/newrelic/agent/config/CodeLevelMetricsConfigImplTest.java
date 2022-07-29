package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CodeLevelMetricsConfigImplTest {

    private Map<String, Object> localProps;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
    }

    @Test
    public void disabledByDefault() {
        CodeLevelMetricsConfigImpl config = new CodeLevelMetricsConfigImpl(localProps);
        assertFalse(config.isEnabled());
    }

    @Test
    public void enabled() {
        localProps.put("code_level_metrics.enabled", true);
        CodeLevelMetricsConfigImpl config = new CodeLevelMetricsConfigImpl(localProps);
        assertFalse(config.isEnabled());
    }

    @Test
    public void canConfigureViaSystemProperty() {
        Properties properties = new Properties();
        //default application_logging is true
        properties.put("newrelic.config.code_level_metrics.enabled", "true");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.emptyMap()) //only test the system property

        ));

        CodeLevelMetricsConfigImpl config = new CodeLevelMetricsConfigImpl(Collections.emptyMap());
        assertTrue(config.isEnabled());

    }

    @Test
    public void canConfigureViaEnvironmentVariables() {

        //default forwarding is false
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(), //use default configs except for forwarding environment variable
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap("NEW_RELIC_CODE_LEVEL_METRICS_ENABLED", "true"))
        ));

        CodeLevelMetricsConfigImpl config = new CodeLevelMetricsConfigImpl(Collections.emptyMap());

        assertTrue(config.isEnabled());

    }
}