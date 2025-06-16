/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.weaver.errorhandler.LogAndReturnOriginal;
import com.newrelic.agent.instrumentation.weaver.extension.CaffeineBackedExtensionClass;
import com.newrelic.agent.instrumentation.weaver.extension.ExtensionHolderFactoryImpl;
import com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPostprocessors;
import com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessors;
import com.newrelic.agent.instrumentation.weaver.preprocessors.TracedWeaveInstrumentationTracker;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.internal.WeavePackageType;
import com.newrelic.bootstrap.BootstrapAgent;
import com.newrelic.bootstrap.EmbeddedJarFilesImpl;
import com.newrelic.weave.ClassWeave;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.weavepackage.CachedWeavePackage;
import com.newrelic.weave.weavepackage.ClassWeavedListener;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;
import com.newrelic.weave.weavepackage.NewClassAppender;
import com.newrelic.weave.weavepackage.PackageWeaveResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import com.newrelic.weave.weavepackage.WeavePackageManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.newrelic.agent.Agent.LOG;
import static com.newrelic.agent.config.SecurityAgentConfig.shouldInitializeSecurityAgent;

/**
 * All interfacing with the weaver is done here.
 */
public class ClassWeaverService implements ClassMatchVisitorFactory, ContextClassTransformer {
    /**
     * Determines how many threads to run in parallel when loading instrumentation packages
     */
    private static final int PARTITIONS = 8;
    private static ClassNode EXTENSION_TEMPLATE;

    static {
        AgentBridge.extensionHolderFactory = new ExtensionHolderFactoryImpl();
        try {
            EXTENSION_TEMPLATE = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                    CaffeineBackedExtensionClass.class.getName(), CaffeineBackedExtensionClass.class.getClassLoader()));
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.WARNING, e,
                    "Unable to initialize custom extension class template. Falling back to default java NewField implementation");
            EXTENSION_TEMPLATE = ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE;
        }
    }

    private final ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> tracedWeaveInstrumentationDetails = new ConcurrentHashMap<>();
    private final WeaveViolationLogger weaveViolationLogger;
    private final AgentWeaverListener listener;
    private final WeavePackageManager weavePackageManager;

    /**
     * Weave Packages loaded from the agent's jar. Cannot be unloaded or reloaded.
     */
    private final Set<String> internalWeavePackages = Sets.newConcurrentHashSet();
    /**
     * Weave Packages loaded dynamically from the extensions folder.
     */
    private final Map<String, String> externalWeavePackages = new ConcurrentHashMap<>();
    private final Instrumentation instrumentation;

    public ClassWeaverService(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.weaveViolationLogger = new WeaveViolationLogger(Agent.LOG);
        this.listener = new AgentWeaverListener(weaveViolationLogger);

        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        ClassTransformerConfig config = agentConfig.getClassTransformerConfig();
        this.weavePackageManager = new WeavePackageManager(listener, instrumentation,
                config.getMaxPreValidatedClassLoaders(), config.preValidateWeavePackages(), config.preMatchWeaveMethods());
    }

    /**
     * Registers the weave instrumentation jars that are packaged into the agent jar's instrumentation directory and
     * present in the extension directory.
     */
    public void registerInstrumentation() {
        loadInternalWeavePackages();
        loadExternalWeavePackages(ServiceFactory.getExtensionService().getWeaveExtensions());
    }

    /**
     * Registers the security weave instrumentation jars that are packaged into the Security agent jar's instrumentation directory.
     */
    public void registerSecurityInstrumentation() {
        if (shouldInitializeSecurityAgent()) {
            loadInternalSecurityWeavePackages();
        }
    }

    public Runnable createRetransformRunnable(Class<?>[] loadedClasses) {
        return new RetransformRunnable(loadedClasses);
    }

    /**
     * Create a weave package from a jar source.
     *
     * @param inputStream The JarInputStream to read from.
     * @param source      URL where the jar was read from.
     */
    private WeavePackage createWeavePackage(InputStream inputStream, String source) throws Exception {
        JarInputStream jarStream = new JarInputStream(inputStream);
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        WeavePackageConfig weavePackageConfig = createWeavePackageConfig(jarStream, source,
                instrumentation, WeavePackageType.INTERNAL, agentConfig);
        ClassTransformerConfig classTransformerConfig = agentConfig.getClassTransformerConfig();

        String weavePackageName = weavePackageConfig.getName();
        if (!classTransformerConfig.isWeavePackageEnabled(weavePackageConfig)) {
            if (weavePackageConfig.isEnabled()) {
                // Only log this if the module has been explicitly disabled (not if it is disabled by default)
                LOG.log(Level.INFO, "Instrumentation {0} is disabled. Skipping.", weavePackageName);
            }
            return null;
        }

        // JAVA-1499 Why o' why do we do this (see JAVA-1445)? The reasons have been lost to the sands of time,
        // but probably are just one of the many quick fixes (aka hacks) to the configuration system to keep two
        // instrumentations from being loaded at the same time. To be fixed as part of JAVA-1499.

        if ("com.newrelic.instrumentation.jcache-1.0.0".equals(weavePackageName)) {
            Boolean jcacheDatastoreEnabled = agentConfig.getValue(
                    "class_transformer.com.newrelic.instrumentation.jcache-datastore-1.0.0.enabled", Boolean.FALSE);
            if (jcacheDatastoreEnabled) {
                LOG.log(Level.INFO, " Instrumentation {0} is disabled since {1} is enabled. Skipping.",
                        weavePackageName, "com.newrelic.instrumentation.jcache-datastore-1.0.0");
                return null;
            }
        }

        WeavePackage weavePackage = CachedWeavePackage.createWeavePackage(new URL(source), jarStream, weavePackageConfig);
        return weavePackage;
    }

    private WeavePackageConfig createWeavePackageConfig(JarInputStream jarStream, String source,
            Instrumentation instrumentation, WeavePackageType type, AgentConfig agentConfig) throws Exception {
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, tracedWeaveInstrumentationDetails);
        AgentPostprocessors postprocessors = new AgentPostprocessors();

        WeavePackageConfig result = WeavePackageConfig.builder()
                .source(source)
                .jarInputStream(jarStream)
                .weavePreprocessor(preprocessors)
                .weavePostprocessor(postprocessors)
                .errorHandleClassNode(LogAndReturnOriginal.ERROR_HANDLER_NODE)
                .extensionClassTemplate(EXTENSION_TEMPLATE)
                .build();

        preprocessors.setInstrumentationTitle(result.getName());

        if (result.getVendorId() != null) {
            // Set the type to "FIELD" for FIT modules. Otherwise this will already be set to INTERNAL or CUSTOM
            type = WeavePackageType.FIELD;
        }
        postprocessors.setWeavePackageType(type);
        return result;
    }

    /**
     * Load all the security weave packages embedded in the agent jar.
     */
    private void loadInternalSecurityWeavePackages() {
        LOG.log(Level.FINE, "Starting security instrumentation load");
        URL securityAgentUrl;
        try {
            securityAgentUrl = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(com.newrelic.bootstrap.BootstrapLoader.NEWRELIC_SECURITY_AGENT).toURI().toURL();
        } catch (Exception err) {
            LOG.log(Level.SEVERE, "Error while loading security instrumentation packages. Security agent jar was not found due to error : {0}",
                    err.getMessage());
            LOG.log(Level.FINE, "Error while loading security instrumentation packages. Security agent jar was not found due to error : {0}", err);
            return;
        }
        Collection<String> jarFileNames = AgentJarHelper.findJarFileNames(securityAgentUrl, Pattern.compile("instrumentation-security\\/(.*).jar"));
        if (jarFileNames.isEmpty()) {
            LOG.log(Level.SEVERE, "No security instrumentation packages were found in the agent.");
        } else {
            LOG.log(Level.FINE, "Loading {0} security instrumentation packages", jarFileNames.size());
        }

        int partitions = Math.min(jarFileNames.size(), PARTITIONS);
        // Note: An ExecutorService would be better suited for this work but we are
        // specifically not using it here to prevent the ConcurrentCallablePointCut
        // from being loaded too early
        final CountDownLatch executorCountDown = new CountDownLatch(partitions);
        List<Set<String>> weavePackagePartitions = partitionInstrumentationJars(jarFileNames, partitions);

        for (final Set<String> weavePackageJars : weavePackagePartitions) {
            Runnable loadWeavePackagesRunnable = () -> {
                try {
                    for (final String name : weavePackageJars) {
                        URL instrumentationUrl = new URL("jar:" + securityAgentUrl.toExternalForm() + "!/" + name);
                        registerInstrumentation(instrumentationUrl);
                    }
                } catch (Throwable t) {
                    LOG.log(Level.FINER, t, "A thread loading weaved packages threw an error");
                } finally {
                    executorCountDown.countDown();
                }
            };

            new Thread(loadWeavePackagesRunnable).start();
        }

        try {
            // Wait for all partitions to complete
            executorCountDown.await();
            LOG.log(Level.FINE, "Loaded {0} internal instrumentation packages", internalWeavePackages.size());
        } catch (InterruptedException e) {
            LOG.log(Level.FINE, e, "Interrupted while waiting for instrumentation packages.");
        }
    }

    /**
     * Register a closable which will run if/when a {@link WeavePackage} is deregistered.
     */
    public void registerInstrumentationCloseable(String instrumentationName, Closeable closeable) {
        WeavePackage weavePackage = weavePackageManager.getWeavePackage(instrumentationName);
        listener.registerInstrumentationCloseable(instrumentationName, weavePackage, closeable);
    }

    /**
     * Load all the weave packages embedded in the agent jar.
     */
    private void loadInternalWeavePackages() {
        Collection<String> jarFileNames = AgentJarHelper.findAgentJarFileNames(Pattern.compile("instrumentation\\/(.*).jar"));
        if (jarFileNames.isEmpty()) {
            LOG.log(Level.SEVERE, "No instrumentation packages were found in the agent.");
        } else {
            LOG.log(Level.FINE, "Loading {0} instrumentation packages", jarFileNames.size());
        }

        int partitions = Math.min(jarFileNames.size(), PARTITIONS);
        // Note: An ExecutorService would be better suited for this work but we are
        // specifically not using it here to prevent the ConcurrentCallablePointCut
        // from being loaded too early
        final CountDownLatch executorCountDown = new CountDownLatch(partitions);
        List<Set<String>> weavePackagePartitions = partitionInstrumentationJars(jarFileNames, partitions);

        for (final Set<String> weavePackageJars : weavePackagePartitions) {

            Runnable loadWeavePackagesRunnable = () -> {
                try {
                    for (final String name : weavePackageJars) {
                        URL instrumentationUrl = BootstrapAgent.class.getResource('/' + name);
                        if (instrumentationUrl == null) {
                            Agent.LOG.error("Unable to find instrumentation jar: " + name);
                        } else {
                            registerInstrumentation(instrumentationUrl);
                        }
                    }
                } catch (Throwable t) {
                    LOG.log(Level.FINER, t, "A thread loading weaved packages threw an error");
                } finally {
                    executorCountDown.countDown();
                }
            };
            new Thread(loadWeavePackagesRunnable).start();
        }

        try {
            // Wait for all partitions to complete
            executorCountDown.await();
            LOG.log(Level.FINE, "Loaded {0} internal instrumentation packages", internalWeavePackages.size());
        } catch (InterruptedException e) {
            LOG.log(Level.FINE, e, "Interrupted while waiting for instrumentation packages.");
        }
    }

    private void registerInstrumentation(URL instrumentationUrl) {
        try (InputStream inputStream = instrumentationUrl.openStream()) {
            WeavePackage internalWeavePackage = createWeavePackage(inputStream, instrumentationUrl.toExternalForm());
            if (null == internalWeavePackage) {
                LOG.log(Level.FINEST, "internal weave package: {0} was null", instrumentationUrl.toExternalForm());
            } else if (internalWeavePackage.getPackageViolations().size() > 0) {
                LOG.log(Level.FINER, "skip loading weave package: {0}", internalWeavePackage.getName());
                for (WeaveViolation violation : internalWeavePackage.getPackageViolations()) {
                    LOG.log(Level.FINER, "\t violation: {0}", violation);
                }
            } else {
                LOG.log(Level.FINER, "adding weave package: {0}", internalWeavePackage.getName());
                internalWeavePackages.add(internalWeavePackage.getName());
                weavePackageManager.register(internalWeavePackage);
            }
        } catch (Throwable t) {
            LOG.log(Level.FINER, t, "unable to load weave package jar {0}", instrumentationUrl);
        }
    }

    private List<Set<String>> partitionInstrumentationJars(Collection<String> jarFileNames, int partitions) {
        List<Set<String>> instrumentationPartitions = new ArrayList<>(partitions);

        // Initialize each partition with an empty set
        for (int i = 0; i < partitions; i++) {
            instrumentationPartitions.add(new HashSet<String>());
        }

        int index = 0;
        for (String jarFileName : jarFileNames) {
            // Attempt to evenly distribute the jarFileNames across the partitions
            instrumentationPartitions.get(index++ % partitions).add(jarFileName);
        }

        return instrumentationPartitions;
    }

    /**
     * Load new instrumentation packages from disk and put any new matchers in the matchers collection.
     *
     * @param weaveExtensions
     */
    private Collection<ClassMatchVisitorFactory> loadExternalWeavePackages(Collection<File> weaveExtensions) {
        Collection<ClassMatchVisitorFactory> matchers = new HashSet<>();

        for (File weaveExtension : weaveExtensions) {
            try (JarInputStream stream = new JarInputStream(new FileInputStream(weaveExtension))) {
                AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
                WeavePackageConfig weaveConfig = createWeavePackageConfig(stream, weaveExtension.getAbsolutePath(),
                        instrumentation, WeavePackageType.CUSTOM, agentConfig);
                ClassTransformerConfig classTransformerConfig = agentConfig.getClassTransformerConfig();
                String instrName = weaveConfig.getName();
                if (weavePackageManager.isRegistered(instrName)) {
                    weavePackageManager.deregister(instrName);
                    this.externalWeavePackages.remove(weaveExtension.getAbsolutePath());
                }

                if (!classTransformerConfig.isWeavePackageEnabled(weaveConfig)) {
                    if (weaveConfig.isEnabled()) {
                        // Only log this if the module has been explicitly disabled (not if it is disabled by default)
                        LOG.log(Level.INFO, "Instrumentation {0} is disabled. Skipping.", instrName);
                    }
                    continue;
                }

                WeavePackage externalPackage = WeavePackage.createWeavePackage(stream, weaveConfig);
                if (externalPackage.getPackageViolations().size() > 0) {
                    LOG.log(Level.FINER, "skip loading external weave package: {0}", instrName);
                    for (WeaveViolation violation : externalPackage.getPackageViolations()) {
                        LOG.log(Level.FINER, "\t{0}", violation);
                    }
                } else {
                    weavePackageManager.register(externalPackage);
                    externalWeavePackages.put(weaveExtension.getAbsolutePath(), weaveConfig.getName());
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, e, "Error reading weave extension {0}", weaveExtension.getAbsolutePath());
            }
        }
        return matchers;
    }

    /**
     * Unload the given external instrumentation packages
     */
    private Collection<ClassMatchVisitorFactory> unloadExternalWeavePackages(Set<String> removedFilePaths) {
        LOG.log(Level.INFO, "ClassWeaveService removing {0} weave packages.", removedFilePaths.size());
        Collection<ClassMatchVisitorFactory> matchers = Sets.newHashSetWithExpectedSize(removedFilePaths.size());

        for (String removedFilePath : removedFilePaths) {
            String weavePackageName = this.externalWeavePackages.get(removedFilePath);
            if (this.internalWeavePackages.contains(weavePackageName)) {
                Agent.LOG.log(Level.FINER, "Attempted to unload internal weave package {0} -- {1}. Ignoring request.",
                        weavePackageName, removedFilePath);
                continue;
            }

            WeavePackage externalPackage = weavePackageManager.deregister(weavePackageName);
            if (null == externalPackage) {
                Agent.LOG.log(Level.FINER,
                        "Attempted to unload non-existent weave package {0} -- {1}. Ignoring request.",
                        weavePackageName, removedFilePath);
            } else {
                externalWeavePackages.remove(removedFilePath);
            }
        }
        return matchers;
    }

    /**
     * Given a set of weave extensions, this method loads them, validates them and provides a runnable which will
     * retransform any classes that match any old or new instrumentation. Any non-instrumentation weave classes will be
     * redefined if they were previously loaded.
     *
     * @return A runnable to retransform all relevant classes and close any closables
     */
    public Runnable reloadExternalWeavePackages(Collection<File> newWeaveExtensions,
            Collection<File> removedWeaveExtensions) {
        loadExternalWeavePackages(newWeaveExtensions);

        Set<String> removedFilePaths = Sets.newHashSetWithExpectedSize(removedWeaveExtensions.size());
        for (File removedFile : removedWeaveExtensions) {
            removedFilePaths.add(removedFile.getAbsolutePath());
        }
        unloadExternalWeavePackages(removedFilePaths);

        Class<?>[] loadedClasses = instrumentation.getAllLoadedClasses();
        return createRetransformRunnable(loadedClasses);
    }

    private final ConcurrentMap<ClassLoader, ClassCache> retransformCaches = new ConcurrentHashMap<>();
    private volatile boolean isRetransforming = false;

    private class RetransformRunnable implements Runnable {

        private final Class[] loadedClasses;

        public RetransformRunnable(Class[] loadedClasses) {
            this.loadedClasses = loadedClasses;
        }

        @Override
        public void run() {
            try {
                isRetransforming = true;
                ServiceFactory.getClassTransformerService().retransformMatchingClassesImmediately(loadedClasses,
                        Sets.<ClassMatchVisitorFactory>newHashSet(ClassWeaverService.this));
            } finally {
                isRetransforming = false;
                retransformCaches.clear();
            }
        }
    }
    // TODO
    private ClassCache getClassCache(ClassLoader loader) {
        if (null == loader) {
            loader = BootstrapLoader.PLACEHOLDER;
        }
        ClassCache cache;
        if (isRetransforming) {
            if (!retransformCaches.containsKey(loader)) {
                if (loader == BootstrapLoader.PLACEHOLDER) {
                    retransformCaches.putIfAbsent(loader, new ClassCache(BootstrapLoader.get()));
                } else {
                    retransformCaches.putIfAbsent(loader, new ClassCache(new ClassLoaderFinder(loader)));
                }
            }
            cache = retransformCaches.get(loader);
            if (null == cache) {
                cache = new ClassCache(new ClassLoaderFinder(loader));
            }
        } else {
            if (loader == BootstrapLoader.PLACEHOLDER) {
                cache = new ClassCache(BootstrapLoader.get());
            } else {
                cache = new ClassCache(new ClassLoaderFinder(loader));
            }
        }
        return cache;
    }

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
            ClassVisitor cv, InstrumentationContext context) {
        // actual matching will be done in the transform method.
        if (isRetransforming) {
            try {
                if (weavePackageManager.match(loader, reader.getClassName(), getClassCache(loader)).size() == 0) {
                    return null;
                }
            } catch (IOException e) {
            }
        }
        context.putMatch(this, null);
        return null;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, final InstrumentationContext context, Match match)
            throws IllegalClassFormatException {

        if (!PointCutClassTransformer.isValidClassName(className)) {
            return null;
        }

        ClassWeavedListener classWeavedCallback = new ClassWeavedListener() {
            @Override
            public void classWeaved(PackageWeaveResult weaveResult, ClassLoader classloader, ClassCache cache) {
                List<WeaveViolation> violations = weaveResult.getValidationResult().getViolations();
                if (!violations.isEmpty()) {
                    // This is due to the nature of annotation weaving happening at weave time instead of validation time
                    weaveViolationLogger.logWeaveViolations(weaveResult.getValidationResult(), classloader, false);
                    return;
                }

                final String packageName = weaveResult.getValidationResult().getWeavePackage().getName();
                if (Agent.LOG.isFinerEnabled()) {
                    try {
                        if (Agent.LOG.isFinerEnabled()) {
                            ClassInformation weavedClass = cache.getClassInformation(weaveResult.getClassName());
                            Agent.LOG.log(Level.FINER, "{0} matched {1}", packageName, weavedClass.className);
                            for (String superName : weavedClass.getAllSuperNames(cache)) {
                                Agent.LOG.log(Level.FINER, "\ts: {0}", superName);
                            }
                            for (String interfaceName : weavedClass.getAllInterfaces(cache)) {
                                Agent.LOG.log(Level.FINER, "\ti: {0}", interfaceName);
                            }
                        }
                    } catch (IOException ioe) {
                        Agent.LOG.log(Level.FINEST, ioe, "exception while getting supertype info");
                    }
                }

                if (weaveResult.weavedClass()) {
                    try {
                        Map<String, byte[]> annotationProxyClasses = weaveResult.getAnnotationProxyClasses();
                        if (!annotationProxyClasses.isEmpty()) {
                            // Special case for annotation weaving in order to support dynamic annotation proxies. We need to add
                            // the dynamic proxy classes that we created to the current classloader at this point
                            if (BootstrapLoader.PLACEHOLDER == classloader) {
                                NewClassAppender.appendClassesToBootstrapClassLoader(
                                        instrumentation,
                                        annotationProxyClasses);
                            } else {
                                NewClassAppender.appendClasses(classloader, annotationProxyClasses);
                            }
                        }
                    } catch (Exception e) {
                        Agent.LOG.log(Level.FINE, e, "Unable to add annotation proxy classes");
                    }

                    String weaveClassStat = MessageFormat.format(MetricNames.SUPPORTABILITY_WEAVE_CLASS,
                            packageName, weaveResult.getClassName());
                    ServiceFactory.getStatsService().doStatsWork(
                            StatsWorks.getRecordMetricWork(weaveClassStat, 1), weaveClassStat);

                    for (String originalName : weaveResult.getWeavedMethods().keySet()) {
                        Agent.LOG.log(Level.FINE, "{0}: weaved target {1}-{2}", packageName, classloader,
                                weaveResult.getClassName());
                        for (Method method : weaveResult.getWeavedMethods().get(originalName)) {
                            Agent.LOG.log(Level.FINE, "\t{0}.{1}:{2}", originalName, method.getName(),
                                    method.getDescriptor());
                            context.addWeavedMethod(method, packageName);
                        }
                        addTraceInformation(ClassWeaverService.this.tracedWeaveInstrumentationDetails,
                                packageName, context, weaveResult.getComposite(), originalName);
                    }
                } else {
                    Agent.LOG.log(Level.FINER, "{0} matched class {1} but no methods were weaved.", packageName,
                            weaveResult.getClassName());
                }
            }
        };
        try {
            // FIXME this was a gross hack to force the OTel AgentClassLoader to be used for Context (which does end up getting weaved but instrumentation still didn't work)
//            if (className.equals("io/opentelemetry/javaagent/shaded/io/opentelemetry/context/Context") && loader == null) {
//                List<Class> filteredSdkTracerProviderClass = Arrays.stream(instrumentation.getAllLoadedClasses())
//                        .filter(c -> c.getName().equals("io.opentelemetry.sdk.trace.SdkTracerProvider"))
//                        .collect(Collectors.toList());
//
//                ClassLoader sdkTracerProviderLoader = filteredSdkTracerProviderClass.get(0).getClassLoader();
//                if (sdkTracerProviderLoader != null) {
//                    loader = sdkTracerProviderLoader;
//                }
//            }

            return weavePackageManager.weave(loader, getClassCache(loader), className, classfileBuffer,
                    context.getSkipMethods(), classWeavedCallback);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * For every tracer that originated from weaved code:
     * <ol>
     * <li>Add WeaveInstrumentation info to the context</li>
     * <li>Remove the tracer from the composite method</li>
     * </ol>
     */
    public static void addTraceInformation(ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> weaveTraceDetailsTrackers,
            String weavePackageName, InstrumentationContext context, ClassNode composite,
            String originalClassName) {
        Set<TracedWeaveInstrumentationTracker> traceDetailsTrackers = weaveTraceDetailsTrackers.get(weavePackageName);
        if (null != traceDetailsTrackers) {
            for (TracedWeaveInstrumentationTracker traceDetails : traceDetailsTrackers) {
                if (weavePackageName.equals(traceDetails.getWeavePackageName())
                        && originalClassName.equals(traceDetails.getClassName())) {

                    List<MethodNode> compositeMethods = getMatches(composite, traceDetails);
                    for (MethodNode compositeMethod : compositeMethods) {

                        if ((compositeMethod.access & Opcodes.ACC_BRIDGE) != 0) {
                            // trace the bridge target if it presides in the same class node
                            Method bridgeTarget = ClassWeave.whereDoesTheBridgeGo(compositeMethod);
                            MethodNode bridgeTargetNode = WeaveUtils.findMatch(composite.methods, bridgeTarget);
                            if (null != bridgeTargetNode) {
                                compositeMethod = bridgeTargetNode;
                            }
                        }

                        Agent.LOG.log(Level.FINER, "Writing TracedWeaveInstrumentation: {0} - {1}.{2}({3})",
                                weavePackageName, composite.name, compositeMethod.name, compositeMethod.desc);
                        traceDetails.addToInstrumentationContext(context, new Method(compositeMethod.name,
                                compositeMethod.desc));
                        TracedWeaveInstrumentationTracker.removeTraceAnnotations(compositeMethod);
                    }
                }
            }
        }
    }

    private static List<MethodNode> getMatches(ClassNode composite, TracedWeaveInstrumentationTracker traceDetails) {
        List<MethodNode> matches = new ArrayList<>();

        if (traceDetails.isWeaveIntoAllMethods()) {
            for (MethodNode method : composite.methods) {
                List<AnnotationNode> methodAnnotations = WeaveUtils.getMethodAnnotations(method);
                for (AnnotationNode methodAnnotation : methodAnnotations) {
                    if (Type.getType(WeaveIntoAllMethods.class).getDescriptor().equals(methodAnnotation.desc)) {
                        matches.add(method);
                    }
                }
            }
        } else {
            MethodNode match = WeaveUtils.findMatch(composite.methods, traceDetails.getMethod());
            if (match != null) {
                matches.add(match);
            }
        }

        return matches;
    }

    public WeavePackageManager getWeavePackageManger() {
        return weavePackageManager;
    }
}
