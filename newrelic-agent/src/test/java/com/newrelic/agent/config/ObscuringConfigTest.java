package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Mocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ObscuringConfigTest {

    private Map<String, Object> obscureConfigProps;
    private Map<String, Object> obscuringKeyConfigProps;
    private final String yamlObscuringKeyValue = "abc123";
    private final String sysPropValue = "key_from_sysProps";
    private final String envPropValue = "key_from_envProps";

    @Before
    public void setup() {
        obscureConfigProps = new HashMap<>();
        obscuringKeyConfigProps = new HashMap<>();
    }

    @After
    public void teardown() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void createsObscureKeyConfigWithValuesFromYaml() {
        ObscuringKeyConfig result = createObscureKeyConfigWithYamlSettings();
        assertEquals(yamlObscuringKeyValue, result.getObscuringKey());
    }

    @Test
    public void checkSystemPropertyObscuringKeyOverridesYaml() {
        setObscuringKeySystemProperty();
        ObscuringKeyConfig result = createObscureKeyConfigWithYamlSettings();
        assertEquals(sysPropValue, result.getObscuringKey());
    }

    @Test
    public void checkEnvironmentPropertyObscuringKeyOverridesYaml() {
        setObscuringKeyEnvironmentProperty();
        ObscuringKeyConfig result = createObscureKeyConfigWithYamlSettings();
        assertEquals(envPropValue, result.getObscuringKey());
    }

    @Test
    public void deobscuresAll() {
//        Map<String, Object> obscuringKeyConfigProps = new HashMap<>();
        obscuringKeyConfigProps.put("obscuring_key", "abc123");
        Map<String, Object> obscuredMap = ImmutableMap.of(
                "config", obscuringKeyConfigProps,
                "top_level", new ObscuredYamlPropertyWrapper("FwMPRFcAU1Y="),
                "in_list", Arrays.asList("a", "b", new ObscuredYamlPropertyWrapper("Ag==")),
                "in_map", Collections.singletonMap("state", new ObscuredYamlPropertyWrapper("LhAGVl1d"))
        );

        ObscuringConfig target = new ObscuringConfig(obscuredMap, "not_applicable");
        Map<String, Object> deobscuredProperties = target.getDeobscuredProperties();

        assertEquals("value324", deobscuredProperties.get("top_level"));
        assertEquals(Arrays.asList("a", "b", "c"), deobscuredProperties.get("in_list"));
        assertEquals(Collections.singletonMap("state", "Oregon"), deobscuredProperties.get("in_map"));
    }

    public ObscuringKeyConfig createObscureKeyConfigWithYamlSettings() {
        obscuringKeyConfigProps.put(ObscuringKeyConfig.OBSCURING_KEY, yamlObscuringKeyValue);
        obscureConfigProps.put(ObscuringConfig.OBSCURING_CONFIG, obscuringKeyConfigProps);
        ObscuringConfig target = new ObscuringConfig(obscureConfigProps, "not_applicable");
        return target.createObscuringKeyConfig();
    }

    private void setObscuringKeySystemProperty() {
        Map<String, String> sysProps = new HashMap<>();
        sysProps.put(ObscuringConfig.SYSTEM_PROPERTY_ROOT + ObscuringKeyConfig.OBSCURING_KEY, sysPropValue);
        Mocks.createSystemPropertyProvider(sysProps);
    }

    private void setObscuringKeyEnvironmentProperty() {
        Map<String, String> sysProps = new HashMap<>();
        Map<String, String> envProps = new HashMap<>();
        envProps.put(ObscuringConfig.SYSTEM_PROPERTY_ROOT + ObscuringKeyConfig.OBSCURING_KEY, envPropValue);
        Mocks.createSystemPropertyProvider(sysProps, envProps);
    }
}