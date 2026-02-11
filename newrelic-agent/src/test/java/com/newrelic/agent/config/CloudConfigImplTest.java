package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class CloudConfigImplTest {

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    public static final String sysPropKey = CloudConfigImpl.SYSTEM_PROPERTY_ROOT + CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED;
    public static final String envVarKey = "NEW_RELIC_CLOUD_METADATA_PROXY_BYPASS_ENABLED";
    public static final Properties systemProps = new Properties();

    @Test
    public void testMetadataProxyBypassEnabled_SetToTrue_ViaYml() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, true);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue(cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SetToFalse_ViaYml() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, false);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse(cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_DefaultFalse() {
        CloudConfigImpl cloudConfig = new CloudConfigImpl(Collections.emptyMap());
        assertFalse(cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_NullProps() {
        CloudConfigImpl cloudConfig = new CloudConfigImpl(null);;
        assertFalse(cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SetToTrue_ViaSysProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, false);

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertTrue("System property should override YML configuration", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SetToFalse_ViaSystemProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, true);

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "false");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertFalse("System property should override YML configuration", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SystemProperty_WithoutYmlValue() {
        Map<String, Object> props = new HashMap<>();

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertTrue("System property should work without YML value", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SystemProperty_WithNullProps() {
        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(null);;
        assertTrue("System property should work with null props", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SetToTrue_ViaEnvironmentVariable() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, false);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertTrue("Environment variable should override YML configuration", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_SetToFalse_ViaEnvironmentVariable() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, true);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "false"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("Environment variable should override YML configuration", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_EnvironmentVariable_WithoutYmlValue() {
        Map<String, Object> props = new HashMap<>();

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Environment variable should work without YML value", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_EnvironmentVariable_WithNullProps() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(null);
        assertTrue("Environment variable should work with null props", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_EnvironmentVariableOverridesSystemProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, false);

        systemProps.setProperty(sysPropKey, "false");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(systemProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true" // Env var says true
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Environment variable should have highest precedence", cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_AllThreeSourcesSet() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, false);

        systemProps.setProperty(sysPropKey, "false");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(systemProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Environment variable should override both system property and YML",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_ServerPropNotOverriddenBySystemProperty() {
        // Server properties should not be overridden by system properties
        Map<String, Object> props = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(true);
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, serverProp);

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "false");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Server property should not be overridden by system property",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_ServerPropNotOverriddenByEnvironmentVariable() {
        Map<String, Object> props = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(true);
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, serverProp);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "false"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Server property should not be overridden by environment variable",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_StringValueTrue() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("String 'true' should be converted to boolean true",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_StringValueFalse() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, "false");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("String 'false' should be converted to boolean false",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_NullValue() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, null);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("Null value should return default (false)",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }

    @Test
    public void testMetadataProxyBypassEnabled_InvalidStringValue() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_PROXY_BYPASS_ENABLED, "invalid");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("Invalid string should return default (false)",
                cloudConfig.isCloudMetadataProxyBypassEnabled());
    }
}