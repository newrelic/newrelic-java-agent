/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.otel;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.bootstrap.BootstrapLoader;
import com.newrelic.bootstrap.EmbeddedJarFilesImpl;
import com.newrelic.weave.utils.WeaveUtils;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.cache.weaklockfree.WeakConcurrentMapCleaner;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import io.opentelemetry.javaagent.tooling.ExtensionClassLoader;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;

import static java.util.logging.Level.SEVERE;

// this class does the work similar to AgentStarterImpl. It adds the otel agent to our classloader, but does not add other functionality from the otel agent.
public class OtelInstrumentationService {
    private final Instrumentation instrumentation;
    private ClassLoader extensionClassLoader;


    public OtelInstrumentationService() {
        this.instrumentation = ServiceFactory.getCoreService().getRealInstrumentation();

        installTransformers();

        try {
            Constructor<EarlyInitAgentConfig> constructor = EarlyInitAgentConfig.class.getDeclaredConstructor(Map.class);
            constructor.setAccessible(true);
            EarlyInitAgentConfig earlyConfig = constructor.newInstance(Collections.emptyMap());
            extensionClassLoader = createExtensionClassLoader(getClass().getClassLoader(), earlyConfig);

            earlyConfig.logEarlyConfigErrorsIfAny();

            // TODO the line below is still not working fully
            // at some points the OTel agent will use Utils.getExtensionClassloader, which is set up in AgentStarterImpl
            // which this class simulates. To be able to have Utils return the correct classloader, this class should
            // implement AgentStarter and this instance should be put in AgentInitializer using reflection.
            // But doing this may cause issues with the classloader fixes already in place and negate some of the benefits
            // of this approach.
            AgentInstaller.installBytebuddyAgent(instrumentation, extensionClassLoader, earlyConfig);
            WeakConcurrentMapCleaner.start();

            // LazyStorage reads system properties. Initialize it here where we have permissions to avoid
            // failing permission checks when it is initialized from user code.
            if (System.getSecurityManager() != null) {
                Context.current();
            }

        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(SEVERE, e, "Unable to start OTel instrumentation service");
        }
    }


    private void installTransformers() {
        // prevents loading InetAddressResolverProvider SPI before agent has started
        // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/7130
        // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/10921
        InetAddressClassFileTransformer transformer = new InetAddressClassFileTransformer();
        instrumentation.addTransformer(transformer, true);
    }

    // copied from AgentStarterImpl
    private static class InetAddressClassFileTransformer implements ClassFileTransformer {
        boolean hookInserted = false;

        @Override
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {
            if (!"java/net/InetAddress".equals(className)) {
                return null;
            }
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor cv =
                    new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cw) {
                        @Override
                        public MethodVisitor visitMethod(
                                int access, String name, String descriptor, String signature, String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if (!"resolver".equals(name)) {
                                return mv;
                            }
                            return new MethodVisitor(api, mv) {
                                @Override
                                public void visitMethodInsn(
                                        int opcode,
                                        String ownerClassName,
                                        String methodName,
                                        String descriptor,
                                        boolean isInterface) {
                                    super.visitMethodInsn(
                                            opcode, ownerClassName, methodName, descriptor, isInterface);
                                    // rewrite Vm.isBooted() to AgentInitializer.isAgentStarted(Vm.isBooted())
                                    if ("jdk/internal/misc/VM".equals(ownerClassName)
                                            && "isBooted".equals(methodName)) {
                                        // need to change this class/method to a new one that we create, probably here in the OtelInstrService
                                        super.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                Type.getInternalName(OtelInstrumentationService.class),
                                                "isAgentStarted",
                                                "(Z)Z",
                                                false);
                                        hookInserted = true;
                                    }
                                }
                            };
                        }
                    };

            cr.accept(cv, 0);

            return hookInserted ? cw.toByteArray() : null;
        }
    }

    private ClassLoader createExtensionClassLoader(ClassLoader agentClassLoader, EarlyInitAgentConfig earlyConfig) throws IOException {
        File javaagentFile = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(BootstrapLoader.OTEL_JAR_NAME);
        boolean isSecurityManagerSupportEnabled = false;
        return ExtensionClassLoader.getInstance(
                agentClassLoader, javaagentFile, isSecurityManagerSupportEnabled, earlyConfig);
    }
}
