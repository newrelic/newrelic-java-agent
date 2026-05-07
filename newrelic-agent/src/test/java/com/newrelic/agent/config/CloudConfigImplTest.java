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

    public static final String sysPropKey = CloudConfigImpl.SYSTEM_PROPERTY_ROOT + CloudConfigImpl.METADATA_BYPASS_PROXY;
    public static final String envVarKey = "NEW_RELIC_CLOUD_METADATA_BYPASS_PROXY";
    public static final Properties systemProps = new Properties();

    @Test
    public void testMetadataBypassProxy_SetToTrue_ViaYml() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, true);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue(cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SetToFalse_ViaYml() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, false);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse(cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_DefaultFalse() {
        CloudConfigImpl cloudConfig = new CloudConfigImpl(Collections.emptyMap());
        assertFalse(cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_NullProps() {
        CloudConfigImpl cloudConfig = new CloudConfigImpl(null);;
        assertFalse(cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SetToTrue_ViaSysProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, false);

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertTrue("System property should override YML configuration", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SetToFalse_ViaSystemProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, true);

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "false");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertFalse("System property should override YML configuration", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SystemProperty_WithoutYmlValue() {
        Map<String, Object> props = new HashMap<>();

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertTrue("System property should work without YML value", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SystemProperty_WithNullProps() {
        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(null);;
        assertTrue("System property should work with null props", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SetToTrue_ViaEnvironmentVariable() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, false);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);;
        assertTrue("Environment variable should override YML configuration", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_SetToFalse_ViaEnvironmentVariable() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, true);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "false"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("Environment variable should override YML configuration", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_EnvironmentVariable_WithoutYmlValue() {
        Map<String, Object> props = new HashMap<>();

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Environment variable should work without YML value", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_EnvironmentVariable_WithNullProps() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(null);
        assertTrue("Environment variable should work with null props", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_EnvironmentVariableOverridesSystemProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, false);

        systemProps.setProperty(sysPropKey, "false");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(systemProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true" // Env var says true
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Environment variable should have highest precedence", cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_AllThreeSourcesSet() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, false);

        systemProps.setProperty(sysPropKey, "false");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(systemProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "true"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Environment variable should override both system property and YML",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_ServerPropNotOverriddenBySystemProperty() {
        // Server properties should not be overridden by system properties
        Map<String, Object> props = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(true);
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, serverProp);

        saveSystemPropertyProviderRule.mockSingleProperty(sysPropKey, "false");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Server property should not be overridden by system property",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_ServerPropNotOverriddenByEnvironmentVariable() {
        Map<String, Object> props = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(true);
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, serverProp);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        envVarKey, "false"
                ))
        ));

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("Server property should not be overridden by environment variable",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_StringValueTrue() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, "true");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertTrue("String 'true' should be converted to boolean true",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_StringValueFalse() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, "false");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("String 'false' should be converted to boolean false",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_NullValue() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, null);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("Null value should return default (false)",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }

    @Test
    public void testMetadataBypassProxy_InvalidStringValue() {
        Map<String, Object> props = new HashMap<>();
        props.put(CloudConfigImpl.METADATA_BYPASS_PROXY, "invalid");

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props);
        assertFalse("Invalid string should return default (false)",
                cloudConfig.isCloudMetadataBypassProxyEnabled());
    }
}