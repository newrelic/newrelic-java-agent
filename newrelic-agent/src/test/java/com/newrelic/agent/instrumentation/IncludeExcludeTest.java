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
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests to ensure that InstrumentationContextManager has the correct include/exclude logic.
 */
public class IncludeExcludeTest {
    private static final ClassLoader SOME_CLASSLOADER = new ClassLoader() {
    };

    private void createServiceManager(Map<String, Object> configMap) throws Exception {
        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
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
    public void testCrypto() throws Exception {
        createServiceManager(createConfigMap());
        InstrumentationContextManager manager = new InstrumentationContextManager(null);

        Assert.assertTrue("Only javax/crypto/ classes should be skipped.", manager.shouldTransform("javax/cryptonotactuallycrypto", SOME_CLASSLOADER));
        Assert.assertFalse("javax/crypto/ classes should be skipped.", manager.shouldTransform("javax/crypto/SomeCryptoClass", SOME_CLASSLOADER));
        Assert.assertFalse("javax/crypto/ classes should be skipped.",
                manager.shouldTransform("javax/crypto/foo/bar/baz/AnotherCryptoClass", SOME_CLASSLOADER));
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

}
