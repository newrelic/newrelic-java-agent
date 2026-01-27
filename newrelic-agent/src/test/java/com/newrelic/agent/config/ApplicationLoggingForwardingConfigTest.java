package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationLoggingForwardingConfigTest {
    private static final int TEST_MAX_SAMPLES_STORED = 5000;
    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();
    private Map<String, Object> localProps;

    @Before
    public void setup() {
        localProps = new HashMap<>();
    }

    @Test
    public void defaultForwardingConfig() {
        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(localProps, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                false);
        assertTrue(config.getEnabled());
        assertFalse(config.isAutoAppNamingAssociationEnabled());
    }

    @Test
    public void testMaxSamplesStoredDefaultValue() {
        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(localProps, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                false);
        assertEquals(ApplicationLoggingForwardingConfig.DEFAULT_MAX_SAMPLES_STORED, config.getMaxSamplesStored());
    }

    @Test
    public void testMaxSamplesStoredDefaultValueIfValueTooLargeForInteger() {
        Map<String, Object> maxSamplesStoreTooLarge = new HashMap<>(localProps);
        maxSamplesStoreTooLarge.put(ApplicationLoggingForwardingConfig.MAX_SAMPLES_STORED, new BigInteger("9999999999999999999999"));

        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(maxSamplesStoreTooLarge,
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                false);
        assertEquals(ApplicationLoggingForwardingConfig.DEFAULT_MAX_SAMPLES_STORED, config.getMaxSamplesStored());
    }

    @Test
    public void testMaxSamplesStoredNotDefaultValue() {
        localProps.put(ApplicationLoggingForwardingConfig.MAX_SAMPLES_STORED, TEST_MAX_SAMPLES_STORED);
        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(localProps, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                false);
        assertEquals(TEST_MAX_SAMPLES_STORED, config.getMaxSamplesStored());
    }

    @Test
    public void usesEnvVarForNestedConfig() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(
                        Collections.singletonMap("NEW_RELIC_APPLICATION_LOGGING_FORWARDING_MAX_SAMPLES_STORED", "5000"))
        ));

        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT, false);
        assertEquals(TEST_MAX_SAMPLES_STORED, config.getMaxSamplesStored());
    }

    @Test
    public void usesSysPropForNestedConfig() {
        Properties properties = new Properties();
        properties.put("newrelic.config.application_logging.forwarding.max_samples_stored", "" + TEST_MAX_SAMPLES_STORED);
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT, false);
        assertEquals(TEST_MAX_SAMPLES_STORED, config.getMaxSamplesStored());
    }

}
