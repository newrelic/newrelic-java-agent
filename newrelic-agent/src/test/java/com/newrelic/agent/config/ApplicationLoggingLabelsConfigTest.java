package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationLoggingLabelsConfigTest {

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule;

    public static final String ENV_VAR_ENABLE = "NEW_RELIC_APPLICATION_LOGGING_FORWARDING_LABELS_ENABLED";
    public static final String ENV_VAR_EXCLUDE = "NEW_RELIC_APPLICATION_LOGGING_FORWARDING_LABELS_EXCLUDE";
    private static final String PARENT_ROOT = "newrelic.config.application_logging.";

    @Before
    public void setup() {
        saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();
    }

    @Test
    public void testDefaultLabelsConfig() {
        Map<String, Object> props = new HashMap<>();

        ApplicationLoggingLabelsConfig labelsConfig = new ApplicationLoggingLabelsConfig(props, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT);

        assertFalse(labelsConfig.getEnabled());
        assertTrue(labelsConfig.getExcludeSet().isEmpty());
    }

    @Test
    public void testSettingExcludeViaSystemProps() {
        Properties props = createSystemProperties("newrelic.config.application_logging.forwarding.labels.exclude", "test1, test2");

        SystemPropertyFactory.setSystemPropertyProvider(
                new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(props), //only test the system property
                        new SaveSystemPropertyProviderRule.TestEnvironmentFacade()));

        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);

        assertTrue(config.getLoggingLabelsExcludeSet().contains("test1"));
        assertTrue(config.getLoggingLabelsExcludeSet().contains("test2"));
        assertFalse(config.getLoggingLabelsExcludeSet().contains("test3"));
    }

    @Test
    public void testSettingExcludesViaEnvVars() {
        Map<String, String> envVars = Collections.singletonMap(ENV_VAR_EXCLUDE, "test1,test2");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envVars)));

        ApplicationLoggingForwardingConfig config = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);

        assertTrue(config.getLoggingLabelsExcludeSet().contains("test1"));
        assertTrue(config.getLoggingLabelsExcludeSet().contains("test2"));
        assertFalse(config.getLoggingLabelsExcludeSet().contains("test3"));
    }

    @Test
    public void testLabelsExcludeDefaultsToEmptySet() {
        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(new HashMap<>(), PARENT_ROOT);
        assertTrue("Excluded set should be empty by default", config.getExcludeSet().isEmpty());
    }

    @Test
    public void canEnableLabelsViaSystemProperty() {
        Properties props = createSystemProperties(PARENT_ROOT + "forwarding.labels.enabled", "true");

        SystemPropertyFactory.setSystemPropertyProvider(
                new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(props), //only test the system property
                        new SaveSystemPropertyProviderRule.TestEnvironmentFacade()));

        ApplicationLoggingForwardingConfig forwardingConfig = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);
        assertTrue(forwardingConfig.isLogLabelsEnabled());
    }

    @Test
    public void canDisableLabelsViaSystemProperty() {
        Properties props = createSystemProperties(PARENT_ROOT + "forwarding.labels.enabled", "false");

        SystemPropertyFactory.setSystemPropertyProvider(
                new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(props), //only test the system property
                        new SaveSystemPropertyProviderRule.TestEnvironmentFacade()));

        ApplicationLoggingForwardingConfig forwardingConfig = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);
        assertFalse(forwardingConfig.isLogLabelsEnabled());
    }

    @Test
    public void canEnableViaEnvironmentVariables() {
        Map<String, String> envVars = Collections.singletonMap(ENV_VAR_ENABLE, "true");

        //default labels is false
        SystemPropertyFactory.setSystemPropertyProvider(
                new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(), //only test the system property
                        new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envVars)));

        ApplicationLoggingForwardingConfig forwardingConfig = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);
        assertTrue(forwardingConfig.isLogLabelsEnabled());
    }

    @Test
    public void canDisableViaEnvironmentVariables() {
        Map<String, String> envVars = Collections.singletonMap(ENV_VAR_ENABLE, "false");

        //default labels is false
        SystemPropertyFactory.setSystemPropertyProvider(
                new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(), //only test the system property
                        new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envVars)));

        ApplicationLoggingForwardingConfig forwardingConfig = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);
        assertFalse(forwardingConfig.isLogLabelsEnabled());
    }

    @Test
    public void canEnableLabelsViaEnvVarsOverSysProps() {
        Map<String, String> envVars = Collections.singletonMap(ENV_VAR_ENABLE, "true");
        Properties props = createSystemProperties(PARENT_ROOT + "forwarding.labels.enabled", "false");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envVars)));

        ApplicationLoggingForwardingConfig forwardingConfig = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);
        assertTrue(forwardingConfig.isLogLabelsEnabled());
    }

    @Test
    public void canDisableLabelsViaEnvVarsOverSysProps() {
        Map<String, String> envVars = Collections.singletonMap(ENV_VAR_ENABLE, "false");
        Properties props = createSystemProperties(PARENT_ROOT + "forwarding.labels.enabled", "true");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envVars)));

        ApplicationLoggingForwardingConfig forwardingConfig = new ApplicationLoggingForwardingConfig(Collections.emptyMap(), PARENT_ROOT, false);
        assertFalse(forwardingConfig.isLogLabelsEnabled());
    }

    @Test
    public void testInitExcludesFromList() {
        Map<String, Object> props = new HashMap<>();
        props.put("exclude", Arrays.asList("exclude1", "exclude2", "exclude3"));

        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT);
        Set<String> excludeSet = config.getExcludeSet();

        assertEquals(3, excludeSet.size());
        assertTrue(excludeSet.contains("exclude1"));
        assertTrue(excludeSet.contains("exclude2"));
        assertTrue(excludeSet.contains("exclude3"));
    }

    @Test
    public void testInitExcludesFromCommaSeparatedString() {
        Map<String, Object> props = new HashMap<>();
        props.put("exclude", "exclude1, exclude2, exclude3");

        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT);
        Set<String> excludeSet = config.getExcludeSet();

        assertEquals(3, excludeSet.size());
        assertTrue(excludeSet.contains("exclude1"));
        assertTrue(excludeSet.contains("exclude2"));
        assertTrue(excludeSet.contains("exclude3"));
    }

    @Test
    public void testInitExcludesHandlesNullsGracefully() {
        Map<String, Object> props = new HashMap<>();
        props.put("exclude", null);

        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT);
        Set<String> excludeSet = config.getExcludeSet();

        assertTrue(excludeSet.isEmpty());
    }

    @Test
    public void testInitExcludesHandlesEmptyList() {
        Map<String, Object> props = new HashMap<>();
        props.put("exclude", Collections.emptyList());

        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT);
        Set<String> excludeSet = config.getExcludeSet();

        assertTrue(excludeSet.isEmpty());
    }

    @Test
    public void testRemoveExcludeLabelsRemoveCorrectly() {
        Map<String, Object> props = new HashMap<>();
        props.put("exclude", Arrays.asList("label4", "label5", "label6"));

        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT);
        Map<String, String> labels = createLabelsMap();
        labels.put("label4", "value4");
        labels.put("label5", "value5");
        labels.put("label6", "value6");

        assertEquals(6, labels.size());
        assertTrue(labels.containsKey("label1"));
        assertTrue(labels.containsKey("label2"));
        assertTrue(labels.containsKey("label3"));
        assertTrue(labels.containsKey("label4"));
        assertTrue(labels.containsKey("label5"));
        assertTrue(labels.containsKey("label6"));

        Map<String, String> filteredLabels = config.removeExcludedLabels(labels);

        assertEquals(3, filteredLabels.size());
        assertTrue(filteredLabels.containsKey("label1"));
        assertTrue(filteredLabels.containsKey("label2"));
        assertTrue(filteredLabels.containsKey("label3"));
    }

    @Test
    public void testRemoveExcludedLabelsWhenNoExcludes() {
        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(new HashMap<>(), PARENT_ROOT);
        Map<String, String> labels = createLabelsMap();
        Map<String, String> filteredLabels = config.removeExcludedLabels(labels);

        assertEquals(3, filteredLabels.size());
        assertTrue(filteredLabels.containsKey("label1"));
        assertTrue(filteredLabels.containsKey("label2"));
        assertTrue(filteredLabels.containsKey("label3"));
    }

    private Properties createSystemProperties(String key, String value) {
        Properties properties = new Properties();
        properties.put(key, value);
        return properties;
    }

    private Map<String, String> createLabelsMap() {
        Map<String, String> labelsMap = new HashMap<>();
        labelsMap.put("label1", "value1");
        labelsMap.put("label2", "value2");
        labelsMap.put("label3", "value3");
        return labelsMap;
    }

}
