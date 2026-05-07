/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Type;

import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.Logger;
import com.newrelic.weave.utils.JarUtils;

public class JarExtensionTest {

    @Test
    public void javaagentExtension() throws IOException {
        ServiceFactory.setServiceManager(Mockito.mock(ServiceManager.class));
        ClassTransformerService classTransformerService = Mockito.mock(ClassTransformerService.class);
        Mockito.when(ServiceFactory.getServiceManager().getClassTransformerService()).thenReturn(
                classTransformerService);

        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Mockito.when(classTransformerService.getExtensionInstrumentation()).thenReturn(instrumentation);

        AgentBridge.agent = Mockito.mock(Agent.class);
        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(AgentBridge.agent.getLogger()).thenReturn(logger);

        JarExtension.create(Mockito.mock(IAgentLogger.class), Mockito.mock(ExtensionParsers.class),
                createJavaAgentExtension(false));

        // Verify that our premain class was invoked
        Mockito.verify(logger, Mockito.times(1)).log(Level.INFO, "My cool extension started! {0}", instrumentation);
    }

    @Test
    public void javaagentExtension_withManifestExtensions() throws Exception {
        ServiceFactory.setServiceManager(Mockito.mock(ServiceManager.class));
        ClassTransformerService classTransformerService = Mockito.mock(ClassTransformerService.class);
        Mockito.when(ServiceFactory.getServiceManager().getClassTransformerService()).thenReturn(
                classTransformerService);

        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Mockito.when(classTransformerService.getExtensionInstrumentation()).thenReturn(instrumentation);

        AgentBridge.agent = Mockito.mock(Agent.class);
        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(AgentBridge.agent.getLogger()).thenReturn(logger);

        Extension extension = new YamlExtension(null, "MyName", null, false);
        ExtensionParsers.ExtensionParser parser = new ExtensionParsers.ExtensionParser() {
            @Override
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                return extension;
            }
        };
        ExtensionParsers extensionParsers = Mockito.mock(ExtensionParsers.class);
        Mockito.when(extensionParsers.getParser(Mockito.any())).thenReturn(parser);

        JarExtension result = JarExtension.create(Mockito.mock(IAgentLogger.class), extensionParsers,
                createJavaAgentExtension(true));

        // Verify that our premain class was invoked
        Mockito.verify(logger, Mockito.times(1)).log(Level.INFO, "My cool extension started! {0}", instrumentation);

        Assert.assertNotNull(result.getClassloader());
        Assert.assertEquals(1, result.getExtensions().size());
        Assert.assertEquals(1, result.getClasses().size());
    }

    private File createJavaAgentExtension(boolean includeExtensions) throws IOException {
        Manifest manifest = new Manifest();

        manifest.getMainAttributes().putValue("Agent-Class", PremainEntry.class.getName());
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        File file = JarUtils.createJarFile("javaagent",
                ImmutableMap.of(Type.getInternalName(PremainEntry.class), Utils.readClass(PremainEntry.class).b),
                manifest,
                includeExtensions ?
                        ImmutableMap.of(Type.getInternalName(ExtensionEntry.class), Utils.readClass(ExtensionEntry.class).b)
                        : null );

        return file;
    }

    private static final class PremainEntry {
        public static void premain(String agentArgs, Instrumentation inst) {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "My cool extension started! {0}", inst);

            // Reference a Caffeine class so the jar gets rewritten.
            Caffeine<?, ?> builder = Caffeine.newBuilder();
        }
    }

    private static final class ExtensionEntry {
        public static void extension() {}
    }

}
