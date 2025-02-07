/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.otel;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.weave.utils.WeaveUtils;
import io.opentelemetry.javaagent.tooling.AgentExtension;
import io.opentelemetry.javaagent.tooling.AgentInstaller;
import io.opentelemetry.javaagent.tooling.AgentStarterImpl;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.javaagent.tooling.ExtensionClassLoader;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.NoopLoggingCustomizer;
import io.opentelemetry.javaagent.tooling.asyncannotationsupport.WeakRefAsyncOperationEndStrategies;
import io.opentelemetry.javaagent.tooling.config.ConfigPropertiesBridge;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
import io.opentelemetry.javaagent.tooling.field.FieldBackedImplementationConfiguration;
import io.opentelemetry.sdk.autoconfigure.SdkAutoconfigureAccess;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilderUtil;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.dynamic.VisibilityBridgeStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.utility.JavaModule;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ServiceLoader;
import java.util.logging.Level;

import static io.opentelemetry.javaagent.tooling.OpenTelemetryInstaller.installOpenTelemetrySdk;
import static io.opentelemetry.javaagent.tooling.SafeServiceLoader.loadOrdered;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

public class OtelInstrumentationService {
    private final Instrumentation instrumentation;
    private ClassLoader extensionClassLoader;


    public OtelInstrumentationService(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;

        installTransformers();

        EarlyInitAgentConfig earlyConfig = EarlyInitAgentConfig.create();
        extensionClassLoader = createExtensionClassLoader(getClass().getClassLoader(), earlyConfig);

        LoggingCustomizer loggingCustomizer = new NewRelicOtelLoggingCustomizer();

        Throwable startupError = null;
        try {
            loggingCustomizer.init(earlyConfig);
            earlyConfig.logEarlyConfigErrorsIfAny();

            AgentInstaller.installBytebuddyAgent(instrumentation, extensionClassLoader, earlyConfig);
            WeakConcurrentMapCleaner.start();

            // LazyStorage reads system properties. Initialize it here where we have permissions to avoid
            // failing permission checks when it is initialized from user code.
            if (System.getSecurityManager() != null) {
                Context.current();
            }
        } catch (Throwable t) {
            // this is logged below and not rethrown to avoid logging it twice
            startupError = t;
        }
        if (startupError == null) {
            loggingCustomizer.onStartupSuccess();
        } else {
            loggingCustomizer.onStartupFailure(startupError);
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
                                                Type.getInternalName(AgentInitializer.class),
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

    private ClassLoader createExtensionClassLoader(
            ClassLoader agentClassLoader, EarlyInitAgentConfig earlyConfig) {
        return ExtensionClassLoader.getInstance(
                agentClassLoader, javaagentFile, isSecurityManagerSupportEnabled, earlyConfig);
    }
}
