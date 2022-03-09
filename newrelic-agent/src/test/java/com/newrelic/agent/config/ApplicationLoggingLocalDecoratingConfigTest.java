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

public class ApplicationLoggingLocalDecoratingConfigTest {

    private Map<String, Object> localProps;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
    }

    @Test
    public void defaultLocalDecoratingConfig() {
        ApplicationLoggingLocalDecoratingConfig config = new ApplicationLoggingLocalDecoratingConfig(localProps,
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);
        assertFalse(config.getEnabled());

    }

    @Test
    public void usesEnvVarForLocalDecoratingConfig() {

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(
                        Collections.singletonMap("NEW_RELIC_APPLICATION_LOGGING_LOCAL_DECORATING_ENABLED", "true"))
        ));

        ApplicationLoggingLocalDecoratingConfig config = new ApplicationLoggingLocalDecoratingConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);
        assertTrue(config.getEnabled());

    }

    @Test
    public void usesSysPropForLocalDecoratingConfig() {
        Properties properties = new Properties();

        properties.put("newrelic.config.application_logging.local_decorating.enabled", "" + "true");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        ApplicationLoggingLocalDecoratingConfig config = new ApplicationLoggingLocalDecoratingConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);
        assertTrue(config.getEnabled());

    }
}