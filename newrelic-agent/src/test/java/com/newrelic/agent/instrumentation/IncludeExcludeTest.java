/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.SecurityAgentConfig;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.module.JarCollectorService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests to ensure that InstrumentationContextManager has the correct include/exclude logic.
 */
public class IncludeExcludeTest {
    private static MockedStatic<SecurityAgentConfig> mockSecurityAgentConfig;
    private static final ClassLoader SOME_CLASSLOADER = new ClassLoader() {
    };

    private void createServiceManager(Map<String, Object> configMap) throws Exception {
        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        JarCollectorService mockJarCollector = serviceManager.getJarCollectorService();
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "staging-collector.newrelic.com");
        map.put("port", 80);
        map.put(AgentConfigImpl.APP_NAME, "Unit test");
        return map;
    }

    @Test
    public void testClassLoader() throws Exception {
        final ClassLoader badClassLoader = new ClassLoader() {
        };
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> classTransformerConfigMap = new HashMap<>();
        classTransformerConfigMap.put("classloader_excludes", badClassLoader.getClass().getName());

        configMap.put("class_transformer", classTransformerConfigMap);
        createServiceManager(configMap);
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue(manager.shouldTransform("a/normal/class/Foo", SOME_CLASSLOADER));
        Assert.assertFalse(manager.shouldTransform("a/normal/class/Foo", badClassLoader));
    }

    @Test
    public void testIncludes() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> classTransformerConfigMap = new HashMap<>();
        classTransformerConfigMap.put("excludes", "^my/custom/.*");
        classTransformerConfigMap.put("includes", "^my/custom/.*");

        configMap.put("class_transformer", classTransformerConfigMap);
        createServiceManager(configMap);
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("config includes should override excludes", manager.shouldTransform("my/custom/Class", SOME_CLASSLOADER));
    }

    @Test
    public void testExcludes() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> classTransformerConfigMap = new HashMap<>();
        classTransformerConfigMap.put("excludes", "^my/custom/.*");

        configMap.put("class_transformer", classTransformerConfigMap);
        createServiceManager(configMap);
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        final String builtInExcludeFile = "/META-INF/excludes"; // exclude file bundled with agent
        Assert.assertFalse(builtInExcludeFile + " classes should be skipped.", manager.shouldTransform("org/objectweb/asm/FunWithBytecode", SOME_CLASSLOADER));
        Assert.assertFalse(builtInExcludeFile + " classes should be skipped.", manager.shouldTransform("some/class/created/ByCGLIB$$/Fun", SOME_CLASSLOADER));

        Assert.assertFalse("config exclude should be skipped", manager.shouldTransform("my/custom/exclude", SOME_CLASSLOADER));
    }

    /*
     * The META-INF/excludes file contains default rules for packages/classes that should be excluded from class transformation.
     * The exclude rules for ^javax/crypto/.*, ^java/security/.*, and ^net/sf/saxon.* interfere with functionality of the security agent
     * and thus are treated differently than other exclude rules depending on whether the security agent is enabled.
     *
     * When the security agent is enabled, these default excludes will be ignored and class transformation will be allowed for any classes in
     * these packages. When the security agent is disabled (as is the default agent behavior) the class_transformer excludes will be applied
     * and will prevent transformation of any classes in these packages.
     *
     * Users can override this behavior by explicitly setting the class_transformer excludes via one of the following config options.
     * This will have the effect applying the exclude rules and preventing transformation of any classes in these packages.
     *
     * YAML:
     *   class_transformer:
     *     excludes: ^javax/crypto/.*,^java/security/.*,^net/sf/saxon.*
     *
     * System property:
     *   -Dnewrelic.config.class_transformer.excludes=^javax/crypto/.*,^java/security/.*,^net/sf/saxon.*
     *
     * Environment variable:
     *   NEW_RELIC_CLASS_TRANSFORMER_EXCLUDES=^javax/crypto/.*,^java/security/.*,^net/sf/saxon.*
     *
     * The tests below represent these special cases.
     */

    @Test
    public void testJavaxCrypto() throws Exception {
        // The default ^javax/crypto/.* exclude in META-INF/excludes should apply. This is default agent behavior.
        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("Only javax/crypto/ classes should be skipped.", manager.shouldTransform("javax/cryptonotactuallycrypto", SOME_CLASSLOADER));
        Assert.assertFalse("javax/crypto/ classes should be skipped.", manager.shouldTransform("javax/crypto/SomeCryptoClass", SOME_CLASSLOADER));
        Assert.assertFalse("javax/crypto/ classes should be skipped.",
                manager.shouldTransform("javax/crypto/foo/bar/baz/AnotherCryptoClass", SOME_CLASSLOADER));
    }

    @Test
    public void testShouldTransformJavaxCryptoWithSecurityAgentEnabled() throws Exception {
        // The default ^javax/crypto/.* exclude in META-INF/excludes should be ignored when the security agent is enabled
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(true);

        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("javax/crypto/ classes should be transformed when the security agent is enabled.",
                manager.shouldTransform("javax/crypto/SomeCryptoClass", SOME_CLASSLOADER));
        Assert.assertTrue("javax/crypto/ classes should be transformed when the security agent is enabled.",
                manager.shouldTransform("javax/crypto/foo/bar/baz/AnotherCryptoClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testShouldNotTransformJavaxCryptoWithSecurityAgentDisabled() throws Exception {
        // The default ^javax/crypto/.* exclude in META-INF/excludes should apply when the security agent is disabled
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(false);

        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertFalse("javax/crypto/ classes should not be transformed when the security agent is disabled.",
                manager.shouldTransform("javax/crypto/SomeCryptoClass", SOME_CLASSLOADER));
        Assert.assertFalse("javax/crypto/ classes should not be transformed when the security agent is disabled.",
                manager.shouldTransform("javax/crypto/foo/bar/baz/AnotherCryptoClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testShouldNotTransformJavaxCryptoWithSecurityAgentEnabledAndExplicitConfigExclude() throws Exception {
        // The default ^javax/crypto/.* exclude in META-INF/excludes should not be ignored when the security agent is enabled
        // and the exclude rule is explicitly added via user config. User config takes precedence.
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(true);

        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> classTransformerConfigMap = new HashMap<>();
        // This simulates setting class_transformer.excludes setting via config
        classTransformerConfigMap.put("excludes", "^javax/crypto/.*");
        configMap.put("class_transformer", classTransformerConfigMap);

        createServiceManager(configMap);
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertFalse("javax/crypto/ classes should be not transformed when the exclude is explicitly set via config, even if security agent is enabled.",
                manager.shouldTransform("javax/crypto/SomeCryptoClass", SOME_CLASSLOADER));
        Assert.assertFalse("javax/crypto/ classes should be not transformed when the exclude is explicitly set via config, even if security agent is enabled.",
                manager.shouldTransform("javax/crypto/foo/bar/baz/AnotherCryptoClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testJavaSecurity() throws Exception {
        // The default ^java/security/.* exclude in META-INF/excludes should apply. This is default agent behavior.
        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("Only java/security/ classes should be skipped.", manager.shouldTransform("java/securitynotactuallysecurity", SOME_CLASSLOADER));
        Assert.assertFalse("java/security/ classes should be skipped.", manager.shouldTransform("java/security/SomeSecurityClass", SOME_CLASSLOADER));
        Assert.assertFalse("java/security/ classes should be skipped.",
                manager.shouldTransform("java/security/foo/bar/baz/AnotherSecurityClass", SOME_CLASSLOADER));
    }

    @Test
    public void testShouldTransformJavaSecurityWithSecurityAgentEnabled() throws Exception {
        // The default ^java/security/.* exclude in META-INF/excludes should be ignored when the security agent is enabled
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(true);

        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("java/security/ classes should be transformed when the security agent is enabled.",
                manager.shouldTransform("java/security/SomeSecurityClass", SOME_CLASSLOADER));
        Assert.assertTrue("java/security/ classes should be transformed when the security agent is enabled.",
                manager.shouldTransform("java/security/foo/bar/baz/AnotherSecurityClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testShouldNotTransformJavaSecurityWithSecurityAgentDisabled() throws Exception {
        // The default ^java/security/.* exclude in META-INF/excludes should apply when the security agent is disabled
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(false);

        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertFalse("java/security/ classes should not be transformed when the security agent is disabled.",
                manager.shouldTransform("java/security/SomeSecurityClass", SOME_CLASSLOADER));
        Assert.assertFalse("java/security/ classes should not be transformed when the security agent is disabled.",
                manager.shouldTransform("java/security/foo/bar/baz/AnotherSecurityClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testShouldNotTransformJavaSecurityWithSecurityAgentEnabledAndExplicitConfigExclude() throws Exception {
        // The default ^java/security/.* exclude in META-INF/excludes should not be ignored when the security agent is enabled
        // and the exclude rule is explicitly added via user config. User config takes precedence.
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(true);

        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> classTransformerConfigMap = new HashMap<>();
        // This simulates setting class_transformer.excludes setting via config
        classTransformerConfigMap.put("excludes", "^java/security/.*");
        configMap.put("class_transformer", classTransformerConfigMap);

        createServiceManager(configMap);
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertFalse("java/security/ classes should be not transformed when the exclude is explicitly set via config, even if security agent is enabled.",
                manager.shouldTransform("java/security/SomeSecurityClass", SOME_CLASSLOADER));
        Assert.assertFalse("java/security/ classes should be not transformed when the exclude is explicitly set via config, even if security agent is enabled.",
                manager.shouldTransform("java/security/foo/bar/baz/AnotherSecurityClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testNetSfSaxon() throws Exception {
        // The default ^net/sf/saxon.* exclude in META-INF/excludes should apply. This is default agent behavior.
        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("Only net/sf/saxon/ classes should be skipped.", manager.shouldTransform("net/sfsaxonnotactuallysaxon", SOME_CLASSLOADER));
        Assert.assertFalse("net/sf/saxon/ classes should be skipped.", manager.shouldTransform("net/sf/saxon/SomeSaxonClass", SOME_CLASSLOADER));
        Assert.assertFalse("net/sf/saxon/ classes should be skipped.", manager.shouldTransform("net/sf/saxon/foo/bar/baz/AnotherSaxonClass", SOME_CLASSLOADER));
    }

    @Test
    public void testShouldTransformNetSfSaxonWithSecurityAgentEnabled() throws Exception {
        // The default ^net/sf/saxon.* exclude in META-INF/excludes should be ignored when the security agent is enabled
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(true);

        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("net/sf/saxon/ classes should be transformed when the security agent is enabled.",
                manager.shouldTransform("net/sf/saxon/SomeSaxonClass", SOME_CLASSLOADER));
        Assert.assertTrue("net/sf/saxon/ classes should be transformed when the security agent is enabled.",
                manager.shouldTransform("net/sf/saxon/foo/bar/baz/AnotherSaxonClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testShouldNotTransformNetSfSaxonWithSecurityAgentDisabled() throws Exception {
        // The default ^net/sf/saxon.* exclude in META-INF/excludes should apply when the security agent is disabled
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(false);

        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertFalse("net/sf/saxon/ classes should not be transformed when the security agent is disabled.",
                manager.shouldTransform("net/sf/saxon/SomeSaxonClass", SOME_CLASSLOADER));
        Assert.assertFalse("net/sf/saxon/ classes should not be transformed when the security agent is disabled.",
                manager.shouldTransform("net/sf/saxon/foo/bar/baz/AnotherSaxonClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }

    @Test
    public void testShouldNotTransformNetSfSaxonWithSecurityAgentEnabledAndExplicitConfigExclude() throws Exception {
        // The default ^net/sf/saxon.* exclude in META-INF/excludes should not be ignored when the security agent is enabled
        // and the exclude rule is explicitly added via user config. User config takes precedence.
        mockSecurityAgentConfig = mockStatic(SecurityAgentConfig.class);
        mockSecurityAgentConfig.when(SecurityAgentConfig::shouldInitializeSecurityAgent).thenReturn(true);

        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> classTransformerConfigMap = new HashMap<>();
        // This simulates setting class_transformer.excludes setting via config
        classTransformerConfigMap.put("excludes", "^net/sf/saxon.*");
        configMap.put("class_transformer", classTransformerConfigMap);

        createServiceManager(configMap);
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertFalse("java/security/ classes should be not transformed when the exclude is explicitly set via config, even if security agent is enabled.",
                manager.shouldTransform("net/sf/saxon/SomeSaxonClass", SOME_CLASSLOADER));
        Assert.assertFalse("java/security/ classes should be not transformed when the exclude is explicitly set via config, even if security agent is enabled.",
                manager.shouldTransform("net/sf/saxon/foo/bar/baz/AnotherSaxonClass", SOME_CLASSLOADER));

        mockSecurityAgentConfig.close();
    }
}
