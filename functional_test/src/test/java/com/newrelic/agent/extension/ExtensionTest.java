/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import com.newrelic.weave.utils.JarUtils;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

public class ExtensionTest {
    public static class JavaAgentExtensionTest implements Callable<Void> {

        public Void call() throws Exception {
            FakeExtensionAgent fakeAgent = new FakeExtensionAgent();
            FakeLogger logger = new FakeLogger();
            fakeAgent.logger = logger;

            AgentBridge.agent = fakeAgent;

            JarExtension jarExtension = JarExtension.create(
                    new FakeAgentLogger(),
                    new ExtensionParsers(Collections.<ConfigurationConstruct>emptyList()),
                    createJavaAgentExtension());

            // Verify that our premain class was invoked
            Assert.assertEquals(Level.INFO, logger.recordedLevel);
            Assert.assertEquals("My cool extension started!", logger.recordedString);

            // now verify that it successfully hooked up a transformer to add a Trace annotation, and that the transformer
            // modified the class bytes before the agent's transformers
            Assert.assertNotNull(getDudeTraceAnnotation());

            return null;
        }

        private Trace getDudeTraceAnnotation() throws NoSuchMethodException, SecurityException {
            return Dude.class.getMethod("instrumentMe").getAnnotation(Trace.class);
        }

        private final class Dude {
            public void instrumentMe() {

            }
        }
    }
    
    public static class NonRetransformPathTest implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            ServiceFactory.getClassTransformerService().getExtensionInstrumentation().addTransformer(
                    getClassFileTransformer("com/newrelic/agent/extension/ExtensionTest$NonRetransformPathTest$Dude2"));
            Assert.assertNotNull(getOtherDudeTraceAnnotation());
            
            return null;
        }

        public static final class Dude2 {
            public void instrumentMe2() {

            }
        }

        private Trace getOtherDudeTraceAnnotation() throws NoSuchMethodException, SecurityException {
            return Dude2.class.getMethod("instrumentMe2").getAnnotation(Trace.class);
        }
    }

    private static File createJavaAgentExtension() throws IOException {
        Manifest manifest = new Manifest();

        manifest.getMainAttributes().putValue("Agent-Class", PremainEntry.class.getName());
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        File file = JarUtils.createJarFile("javaagent", ImmutableMap.of(Type.getInternalName(PremainEntry.class),
                readClass(PremainEntry.class).b), manifest);

        return file;
    }

    private static ClassFileTransformer getClassFileTransformer(final String theClassName) {
        return new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (theClassName.equals(className)) {
                    ClassReader reader = new ClassReader(classfileBuffer);
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

                    ClassVisitor cv = cw;
                    cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                            mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {

                                @Override
                                public void visitCode() {

                                    super.visitAnnotation(Type.getDescriptor(Trace.class), true).visitEnd();

                                    super.visitCode();

                                }

                            };
                            return mv;
                        }

                    };

                    reader.accept(cv, ClassReader.SKIP_FRAMES);

                    return cw.toByteArray();
                }
                return null;
            }
        };

    }

    private static final class PremainEntry {
        public static void premain(String agentArgs, Instrumentation inst) {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "My cool extension started!");

            inst.addTransformer(getClassFileTransformer("com/newrelic/agent/extension/ExtensionTest$JavaAgentExtensionTest$Dude"), true);
        }
    }

    public static ClassReader readClass(Class<?> theClass) throws IOException {
        URL resource = getClassResource(theClass.getClassLoader(), Type.getInternalName(theClass));
        return getClassReaderFromResource(theClass.getName(), resource);
    }

    private static URL getClassResource(ClassLoader loader, String internalClassName) {
        if (loader == null) {
            loader = AgentBridge.getAgent().getClass().getClassLoader();
        }
        URL url = loader.getResource(getClassResourceName(internalClassName));
        if (url == null) {
            try {
                java.lang.reflect.Method getBootstrapResourceMethod = ClassLoader.class.getDeclaredMethod(
                        "getBootstrapResource", String.class);
                getBootstrapResourceMethod.setAccessible(true);
                getBootstrapResourceMethod.invoke(null, "dummy");

                url = (URL) getBootstrapResourceMethod.invoke(null, getClassResourceName(internalClassName));
            } catch (Exception e) {
                url = null;
            }
        }
        return url;
    }

    private static ClassReader getClassReaderFromResource(String internalClassName, URL resource) throws IOException {
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                return new ClassReader(stream);
            }
        } else {
            throw new IOException("Unable to get the resource stream for class " + internalClassName);
        }
    }

    /**
     * java.lang.Class -> java/lang/Class.class
     */
    private static String getClassResourceName(String binaryName) {
        if (binaryName.endsWith(".class"))
            return binaryName;
        else
            return binaryName.replace('.', '/') + ".class";
    }
}
