/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.instrumentation.ClassLoaderClassFinder;
import com.newrelic.agent.instrumentation.builtin.AgentClassLoaderBaseInstrumentation;
import com.newrelic.agent.instrumentation.builtin.AgentClassLoaderInstrumentation;
import com.newrelic.agent.instrumentation.builtin.ClassLoaderPackageAccessInstrumentation;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.weaver.errorhandler.LogAndReturnOriginal;
import com.newrelic.agent.instrumentation.weaver.extension.CaffeineBackedExtensionClass;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;
import com.newrelic.weave.weavepackage.NewClassAppender;
import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.PackageWeaveResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Patch all ClassLoaders so that each version of the loadClass method checks to see if the class name starts with
 * "com.newrelic.". If it does we delegate to the Agent classloader to allow the classes to load properly on any
 * ClassLoader.
 *
 * We first allow the classloader to try to load all classes (except our api classes), but anytime the original
 * loadClass method throws a ClassNotFoundException we catch it and try to load the class through our classloader if
 * it's one of ours. The reason we first let the classloader try to load the class is because some of our new weaved
 * classes must be loaded through the classloader they're added to because they reference classes that can only be
 * resolved through the original classloader.
 *
 * We always try to load our api classes through our classloader because there may be multiple copies of the api jar but
 * we always want the agent's version to take precedence.
 *
 * This instrumentation is required to run the {@link com.newrelic.agent.instrumentation.ClassTransformerService}.
 * It can only be turned off by disabling the ClassTransformerService.
 */
public class ClassLoaderClassTransformer implements ClassMatchVisitorFactory, ContextClassTransformer,
        ClassFileTransformer {

    private static final String agentClassloaderName = Type.getType(AgentBridge.getAgent().getClass().getClassLoader().getClass()).getInternalName();

    /**
     * com.ibm.oti.vm.BootstrapClassLoader = IBM's system classloader
     *
     * sun.reflect.misc.MethodUtil = internal sun reflection class which extends java.lang.ClassLoader in order to
     * perform reflection security checks. Not used in a way that we want to patch. SpringAOP's use of MethodUtil is
     * suspected to be the root cause of JAVA-1415.
     */
    private final Set<String> classloadersToSkip;

    /**
     * There are some class loaders (JBoss modular class loader, for one) that do non-standard things to load classes
     * and this class is unable to catch those class loads without the help of this list.
     */
    private final Set<String> classloadersToInclude;

    private final Instrumentation instrumentation;

    // This package is used to match all sub-classes of "java.lang.ClassLoader" (not inclusive)
    private final WeavePackage classloaderBasePackage;

    // This package is used to match one of the loadClass() methods on "java.lang.ClassLoader" only
    private final WeavePackage classloaderPackage;

    // This package is used to match the checkPackageAccess method on "java.lang.ClassLoader" only
    private final WeavePackage checkPackageAccessPackage;

    /**
     * This map contains all instances of "java.lang.ClassLoader" that we've seen in the system and maps their binary
     * name to the bytes of the Class itself. This allows us to map the full ClassLoader hierarchy in the system
     */
    private final Map<String, byte[]> observedClassLoaders = new ConcurrentHashMap<>();

    private ClassNode extensionTemplate;

    public ClassLoaderClassTransformer(InstrumentationProxy instrumentation, Set<String> classloaderDelegationExcludes,
            Set<String> classloaderDelegationIncludes) {
        classloadersToSkip = ImmutableSet.<String>builder()
                .add("com/ibm/oti/vm/BootstrapClassLoader")
                .add("jdk/internal/loader/BuiltinClassLoader")
                .add("sun/reflect/misc/MethodUtil")
                .add(agentClassloaderName)
                .addAll(classloaderDelegationExcludes)
                .build();
        classloadersToInclude = ImmutableSet.<String>builder()
                .add("org/jboss/modules/NamedClassLoader")
                .add("org/jboss/modules/ConcurrentClassLoader")
                .add("org/jboss/modules/ModuleClassLoader")
                .addAll(classloaderDelegationIncludes)
                .build();

        AgentBridge.getAgent().getLogger().log(Level.FINER, "classloadersToSkip: {0}", classloadersToSkip);

        // Try to use a custom Caffeine based extension template to make NewField access better
        try {
            extensionTemplate = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                    CaffeineBackedExtensionClass.class.getName(), CaffeineBackedExtensionClass.class.getClassLoader()));
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.WARNING, e, "Unable to initialize custom extension class "
                    + "template. Falling back to default java NewField implementation");
            extensionTemplate = ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE;
        }

        this.instrumentation = instrumentation;

        // Build up custom WeavePackages that contain our ClassLoader patching weave code
        classloaderBasePackage = buildBaseClassLoaderPatcherPackage();
        classloaderPackage = buildClassLoaderPatcherPackage();
        checkPackageAccessPackage = buildCheckAccessPatcherPackage();
    }

    private WeavePackage buildBaseClassLoaderPatcherPackage() {
        WeavePackageConfig baseClassloaderPatcher = WeavePackageConfig.builder()
                .name("base-agent-classloader-patcher")
                .errorHandleClassNode(LogAndReturnOriginal.ERROR_HANDLER_NODE)
                .extensionClassTemplate(extensionTemplate)
                .build();

        List<byte[]> baseWeavePackageBytes = new ArrayList<>();
        try {
            // Grab the bytes of our Agent Classloader instrumentation class. Note: This call uses "findResource" but
            // this is ok because we are not under a classloader lock at this point, we are still in the premain()
            byte[] baseClassloaderPatcherInstrumentationBytes = WeaveUtils.getClassBytesFromClassLoaderResource(
                    AgentClassLoaderBaseInstrumentation.class.getName(), ClassLoaderClassTransformer.class.getClassLoader());
            baseWeavePackageBytes.add(baseClassloaderPatcherInstrumentationBytes);
        } catch (IOException e) {
            Agent.LOG.log(Level.FINE, e, "Unable to initialize agent classloader instrumentation");
        }

        return new WeavePackage(baseClassloaderPatcher, baseWeavePackageBytes);
    }

    private WeavePackage buildClassLoaderPatcherPackage() {
        WeavePackageConfig classloaderPatcher = WeavePackageConfig.builder()
                .name("agent-classloader-patcher")
                .errorHandleClassNode(LogAndReturnOriginal.ERROR_HANDLER_NODE)
                .extensionClassTemplate(extensionTemplate)
                .build();

        List<byte[]> weavePackageBytes = new ArrayList<>();
        try {
            // Grab the bytes of our Agent Classloader instrumentation class. Note: This call uses "findResource" but
            // this is ok because we are not under a classloader lock at this point, we are still in the premain()
            byte[] classloaderPatcherInstrumentationBytes = WeaveUtils.getClassBytesFromClassLoaderResource(
                    AgentClassLoaderInstrumentation.class.getName(), ClassLoaderClassTransformer.class.getClassLoader());
            weavePackageBytes.add(classloaderPatcherInstrumentationBytes);
        } catch (IOException e) {
            Agent.LOG.log(Level.FINE, e, "Unable to initialize agent classloader instrumentation");
        }

        return new WeavePackage(classloaderPatcher, weavePackageBytes);
    }

    private WeavePackage buildCheckAccessPatcherPackage() {
        WeavePackageConfig checkAccessPatcher = WeavePackageConfig.builder()
                .name("check-access-patcher")
                .errorHandleClassNode(LogAndReturnOriginal.ERROR_HANDLER_NODE)
                .extensionClassTemplate(extensionTemplate)
                .build();

        List<byte[]> checkAccessPackageBytes = new ArrayList<>();
        try {
            // We only want to add the following instrumentation if the SecurityManager is enabled because it allows
            // us to selectively get around security manager checks from instrumentation modules (if config is enabled)
            if (System.getSecurityManager() != null) {
                ClassTransformerConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig();
                if (config.isGrantPackageAccess()) {
                    // Grab the bytes of our Agent Classloader instrumentation class. Note: This call uses "findResource" but
                    // this is ok because we are not under a classloader lock at this point, we are still in the premain()
                    byte[] classloaderPackageAccessInstrumentationBytes = WeaveUtils.getClassBytesFromClassLoaderResource(
                            ClassLoaderPackageAccessInstrumentation.class.getName(),
                            ClassLoaderClassTransformer.class.getClassLoader());
                    checkAccessPackageBytes.add(classloaderPackageAccessInstrumentationBytes);

                    return new WeavePackage(checkAccessPatcher, checkAccessPackageBytes);
                }
            }
        } catch (IOException e) {
            Agent.LOG.log(Level.FINE, e, "Unable to initialize agent classloader instrumentation");
        }

        return null;
    }

    public void start(Class<?>[] loadedClasses) {
        instrumentation.addTransformer(this, true);

        /*
         * First, lets find every instance of "java.lang.ClassLoader" in the system and do two things:
         *
         * 1. Record the name of the class + class bytes[] in our "observedClassLoaders" Map.
         *
         * 2. If the ClassLoader isn't one of our skipped classloaders store a reference so we can retransform it
         */
        List<Class<?>> toRetransform = new ArrayList<>();
        for (Class<?> clazz : loadedClasses) {
            if (ClassLoader.class.isAssignableFrom(clazz)) {
                try {
                    byte[] classLoaderBytes = Streams.getClassBytes(clazz);
                    if (classLoaderBytes != null) {
                        observedClassLoaders.put(WeaveUtils.getClassInternalName(clazz.getName()), classLoaderBytes);

                        String className = Type.getType(clazz).getInternalName();
                        if (!classloadersToSkip.contains(className) && !className.equals("java/lang/ClassLoader")) {
                            toRetransform.add(clazz);
                        }
                    }
                } catch (IOException e) {
                    Agent.LOG.log(Level.FINE, e, "Unable to capture ClassLoader information for {0}", clazz.getName());
                }
            }
        }

        /*
         * Second, retransform any ClassLoaders that we encountered from step 1. This will use the manual instrumentation
         * that we created and hooked up in the constructor: {@link #ClassLoaderClassTransformer(InstrumentationContextManager)}
         */
        if (!toRetransform.isEmpty()) {
            Agent.LOG.log(Level.FINER, "Retransforming {0}", toRetransform.toString());
            for (Class<?> classloader : toRetransform) {
                try {
                    instrumentation.retransformClasses(classloader);
                } catch (Throwable t) {
                    Agent.LOG.log(Level.FINE, t, "ClassLoaderClassTransformer: Error retransforming {0}", classloader.getName());
                }
            }
        }

        try {
            Agent.LOG.log(Level.FINER, "ClassLoaderClassTransformer: Attempting to redefine {0}", ClassLoader.class);
            InstrumentationProxy.forceRedefinition(instrumentation, ClassLoader.class);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "ClassLoaderClassTransformer: Error redefining {0}", ClassLoader.class.getName());
        }
    }

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
            ClassVisitor cv, InstrumentationContext context) {
        String superName = reader.getClassName().equals("java/lang/ClassLoader") ? reader.getClassName() : reader.getSuperName();
        if (observedClassLoaders.containsKey(superName) ||
                classloadersToInclude.contains(reader.getClassName()) || classloadersToInclude.contains(superName)) {
            // The value (Match) isn't important here so we just pass null. We've matched
            // a known classloader type so lets run this class through the transformer below
            context.putMatch(this, null);
        }
        return null;
    }

    // This method is only used temporarily until the InstrumentationContextManager is available
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassReader reader = new ClassReader(classfileBuffer);
        String superName = reader.getClassName().equals("java/lang/ClassLoader") ? reader.getClassName() : reader.getSuperName();
        if (observedClassLoaders.containsKey(superName) ||
                classloadersToInclude.contains(reader.getClassName()) || classloadersToInclude.contains(superName)) {
            return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, null, null);
        }
        return null;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, final InstrumentationContext context, Match match)
            throws IllegalClassFormatException {
        if (classloadersToSkip.contains(className)) {
            Agent.LOG.log(Level.FINEST, "ClassLoaderClassTransformer: classloadersToSkip contains {0}", className);
            return null;
        }

        // For any new ClassLoaders that we encounter, we should add them to the map
        if (!observedClassLoaders.containsKey(className)) {
            observedClassLoaders.put(className, classfileBuffer);
        }


        try {
            if (loader == null) {
                loader = BootstrapLoader.PLACEHOLDER;
            }

            Agent.LOG.log(Level.FINER, "ClassLoaderClassTransformer transforming: {0} -- {1}", loader, className);

            // This ClassCache will only consult the map of the observedClassLoaders that we pass in, so
            // we don't need to worry about it possibly using findResource() when we call validate(cache).
            ClassCache cache = new ClassCache(new ClassLoaderClassFinder(observedClassLoaders));
            PackageValidationResult result;
            if (className.equals("java/lang/ClassLoader")) {
                // For "java.lang.ClassLoader" we only want to instrument one of the loadClass() methods
                result = classloaderPackage.validate(cache);
            } else {
                // For every other ClassLoader that extends "java.lang.ClassLoader" we want to instrument both loadClass() methods
                result = classloaderBasePackage.validate(cache);
            }

            if (result.succeeded()) {

                Map<String, byte[]> utilityClassBytes = result.computeUtilityClassBytes(cache);
                if (utilityClassBytes != null && !utilityClassBytes.isEmpty()) {
                    // Note: This is a duplication of code from the WeavePackageManager. This is duplicated here because we are
                    // not using WeavePackageManager directly.
                    NewClassAppender.appendClassesToBootstrapClassLoader(instrumentation, utilityClassBytes);
                }

                // Since we know that this class validated we are assured that this class extends
                // "java.lang.ClassLoader" so we can manually set the super class name to "java/lang/ClassLoader" or
                // to empty if it's the java.lang.ClassLoader instance itself.
                final String[] superNames;
                if (className.equals("java/lang/ClassLoader")) {
                    superNames = new String[0];
                } else {
                    superNames = new String[] { "java/lang/ClassLoader" };
                }

                Map<Method, Collection<String>> skipMethods = (context != null)
                  ? context.getSkipMethods()
                  : Collections.emptyMap();

              // This applies the "checkPackageAccess" weaved code only if it should be enabled from the constructor above
                byte[] newBytes = classfileBuffer;
                if (checkPackageAccessPackage != null && newBytes != null && className.equals("java/lang/ClassLoader")) {
                    PackageValidationResult checkPackageAccessResult = checkPackageAccessPackage.validate(cache);
                    if (checkPackageAccessResult.succeeded()) {
                        newBytes = checkPackageAccessResult.weave(className, superNames, new String[0], newBytes, cache, skipMethods).getCompositeBytes(cache);
                    } else {
                        logClassLoaderWeaveViolations(checkPackageAccessResult, className);
                    }

                    if (newBytes == null) {
                        newBytes = classfileBuffer;
                    }
                }

              PackageWeaveResult packageWeaveResult = result.weave(className, superNames, new String[0], newBytes,
                                                                   cache, skipMethods);
                // Do the weaving and use our "non-findResource" cache from above
                newBytes = packageWeaveResult.getCompositeBytes(cache);
                if (className.equals("java/lang/ClassLoader")){
                    //WeaveUtils.printClassFrames(newBytes);
                    //WeaveUtils.createReadableClassFileFromByteArray(newBytes, className, "ClassLoader", "/Users/katherineanderson/Downloads");
                    //VerifierImpl.verify(ClassFile(newBytes), s -> System.out.println(s));
                }

                if (newBytes != null) {
                    Agent.LOG.log(Level.FINE, "ClassLoaderClassTransformer patched {0} -- {1}", loader, className);
                    return newBytes;
                }
            } else {
                logClassLoaderWeaveViolations(result, className);
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINE, t, "ClassLoaderClassTransformer unable to instrument classloader {0} -- {1}", loader, className);
        }

        Agent.LOG.log(Level.FINE, "ClassLoaderClassTransformer skipped transformation: {0} -- {1}", loader, className);
        return null;
    }

    private void logClassLoaderWeaveViolations(PackageValidationResult result, String classLoaderClassName) {
        String weavePackageName = result.getWeavePackage().getName();
        List<WeaveViolation> violations = result.getViolations();

        Agent.LOG.log(Level.FINE, "{0} - {1} violations against classloader {2}", weavePackageName,
                violations.size(), classLoaderClassName);
        for (WeaveViolation violation : violations) {
            Agent.LOG.log(Level.FINE, "WeaveViolation: {0}", violation.getType().name());
            Agent.LOG.log(Level.FINE, "\t\tClass: {0}", violation.getClazz());
            if (violation.getMethod() != null) {
                Agent.LOG.log(Level.FINE, "\t\tMethod: {0}", violation.getMethod());
            }
            if (violation.getField() != null) {
                Agent.LOG.log(Level.FINE, "\t\tField: {0}", violation.getField());
            }
            Agent.LOG.log(Level.FINE, "\t\tReason: {0}", violation.getType().getMessage());
        }
    }
}
