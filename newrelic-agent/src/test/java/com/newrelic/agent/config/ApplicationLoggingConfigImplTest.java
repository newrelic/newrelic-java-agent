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

public class ApplicationLoggingConfigImplTest {

    private Map<String, Object> localProps;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
    }

    @Test
    public void testShouldBeEnabled() {
        ApplicationLoggingConfigImpl config = new ApplicationLoggingConfigImpl(localProps, false);
        assertTrue(config.isEnabled());
    }

    @Test
    public void testDisabledOrNotWithHighSecurity() {
        ApplicationLoggingConfigImpl config = new ApplicationLoggingConfigImpl(localProps, true);
        assertTrue(config.isEnabled());
        assertTrue(config.isMetricsEnabled());

        assertFalse(config.isLocalDecoratingEnabled());
        assertFalse(config.isForwardingEnabled());

    }

    @Test
    public void canConfigureViaSystemProperty() {
        Properties properties = new Properties();
        //default application_logging is true
        properties.put("newrelic.config.application_logging.enabled", "false");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.emptyMap()) //only test the system property

        ));

        ApplicationLoggingConfigImpl config = new ApplicationLoggingConfigImpl(Collections.emptyMap(), false);
        assertFalse(config.isEnabled());

    }

    @Test
    public void canConfigureViaEnvironmentVariables() {

        //default forwarding is false
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(), //use default configs except for forwarding environment variable
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap("NEW_RELIC_APPLICATION_LOGGING_FORWARDING_ENABLED", "true"))
        ));

        ApplicationLoggingConfigImpl config = new ApplicationLoggingConfigImpl(Collections.emptyMap(), false);

        assertTrue(config.isForwardingEnabled());

    }
}