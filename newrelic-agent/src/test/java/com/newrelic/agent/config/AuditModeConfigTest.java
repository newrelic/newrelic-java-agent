package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AuditModeConfigTest {
    @Test
    public void constructors_properlySetEnabledFlag() {
        AuditModeConfig auditModeConfig = new AuditModeConfig(true, true);
        Assert.assertTrue(auditModeConfig.isEnabled());

        auditModeConfig = new AuditModeConfig(false, true);
        Assert.assertTrue(auditModeConfig.isEnabled());

        auditModeConfig = new AuditModeConfig(true, false);
        Assert.assertTrue(auditModeConfig.isEnabled());

        auditModeConfig = new AuditModeConfig(false, false);
        Assert.assertFalse(auditModeConfig.isEnabled());

        // Map constructor
        Map<String, Object> props = new HashMap<>();
        props.put(AuditModeConfig.ENABLED, "true");
        auditModeConfig = new AuditModeConfig(props);
        Assert.assertTrue(auditModeConfig.isEnabled());

        props.put(AuditModeConfig.ENABLED, "false");
        auditModeConfig = new AuditModeConfig(props);
        Assert.assertFalse(auditModeConfig.isEnabled());

        props.put(AuditModeConfig.ENABLED, "");
        auditModeConfig = new AuditModeConfig(props);
        Assert.assertFalse(auditModeConfig.isEnabled());
    }

    @Test
    public void constructor_populatesEndpoints() {
        Map<String, Object> props = new HashMap<>();
        props.put(AuditModeConfig.ENDPOINTS, "one,two,three,four");

        AuditModeConfig auditModeConfig = new AuditModeConfig(props);
        Set<String> endpoints = auditModeConfig.getEndpoints();
        Assert.assertEquals(4, endpoints.size());
        Assert.assertTrue(endpoints.contains("one"));
        Assert.assertTrue(endpoints.contains("two"));
        Assert.assertTrue(endpoints.contains("three"));
        Assert.assertTrue(endpoints.contains("four"));
    }
}
