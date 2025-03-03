/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.*;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.instrumentation.weaver.ClassLoaderClassTransformer;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.security.instrumentation.helpers.ThreadLocalLockHelper;
import com.newrelic.api.agent.security.schema.SecurityMetaData;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.commons.Method;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ClassTransformerServiceImpl extends AbstractService implements ClassTransformerService {

    private final boolean isEnabled;
    private volatile PointCutClassTransformer classTransformer;
    private volatile ClassRetransformer localRetransformer;
    private volatile ClassRetransformer remoteRetransformer;
    private final List<ClassFileTransformer> classTransformers = Collections.synchronizedList(new ArrayList<ClassFileTransformer>());
    private final long shutdownTime;
    private InstrumentationContextManager contextManager;
    private TraceMatchTransformer traceMatchTransformer;
    private ClassLoaderClassTransformer classLoaderClassTransformer = null;
    private final InstrumentationImpl instrumentation;
    private final ScheduledExecutorService executor;
    private final Instrumentation extensionInstrumentation;
    private final AtomicReference<Set<ClassMatchVisitorFactory>> retransformClassMatchers = new AtomicReference<>(
            createRetransformClassMatcherList());

    public ClassTransformerServiceImpl(InstrumentationProxy instrumentationProxy)
            throws Exception {
        super(ClassTransformerServiceImpl.class.getSimpleName());

        ClassTransformerConfig classTransformerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getClassTransformerConfig();
        isEnabled = classTransformerConfig.isEnabled();

        if (isEnabled) {
            this.classLoaderClassTransformer = new ClassLoaderClassTransformer(instrumentationProxy,
                    classTransformerConfig.getClassloaderDelegationExcludes(), classTransformerConfig.getClassloaderDelegationIncludes());
            classLoaderClassTransformer.start(instrumentationProxy.getAllLoadedClasses());
        }

        // ExtensionInstrumentation registers two class transformers so that any class transformer registered by a jar
        // extension is hooked up in front of our class transformers so that it can add Trace annotations which we then
        // pick up
        extensionInstrumentation = new ExtensionInstrumentation(instrumentationProxy);

        instrumentation = new InstrumentationImpl(logger, classTransformerConfig);
        AgentBridge.instrumentation = instrumentation;

        ThreadFactory factory = new DefaultThreadFactory("New Relic Retransformer", true);
        executor = Executors.newSingleThreadScheduledExecutor(factory);

        long shutdownDelayInNanos = classTransformerConfig.getShutdownDelayInNanos();
        if (shutdownDelayInNanos > 0L) {
            shutdownTime = System.nanoTime() + shutdownDelayInNanos;
            final String msg = MessageFormat.format(
                    "The Class Transformer Service will stop instrumenting classes after {0} secs",
                    TimeUnit.SECONDS.convert(shutdownDelayInNanos, TimeUnit.NANOSECONDS));
            getLogger().info(msg);
        } else {
            shutdownTime = Long.MAX_VALUE; // Agent fails after 291 years, 8 months, 2 days.
        }
    }

    private Set<ClassMatchVisitorFactory> createRetransformClassMatcherList() {
        return Sets.newSetFromMap(new ConcurrentHashMap<ClassMatchVisitorFactory, Boolean>());
    }

    @Override
    protected void doStart() throws Exception {
        if (!isEnabled()) {
            getLogger().info("The class transformer is disabled.  No classes will be instrumented.");
            return;
        }
        InstrumentationProxy instrProxy = ServiceFactory.getCoreService().getInstrumentation();
        if (instrProxy == null) {
            getLogger().severe("Unable to initialize the class transformer because there is no instrumentation hook");
        } else {
            classTransformer = startClassTransformer(instrProxy);
        }
        queueRetransform();
    }

    private void queueRetransform() {
        executor.schedule(() -> retransformMatchingClasses(), getRetransformPeriodInSeconds(), TimeUnit.SECONDS);
    }

    private long getRetransformPeriodInSeconds() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getValue(
                "class_transformer.retransformation_period", 10l);
    }

    @Override
    public void checkShutdown() {
        if (shutdownTime == Long.MAX_VALUE || isStopped()) {
            return;
        }
        // Provide more transform info before shutting down. Helpful for support.
        final long nsTilShutdown = shutdownTime - System.nanoTime();
        if (nsTilShutdown < 0) {
            try {
                getLogger().info("Stopping Class Transformer Service based on configured shutdown_delay");
                stop();
            } catch (Exception e) {
                final String msg = MessageFormat.format("Failed to stop Class Transformer Service: {0}", e);
                getLogger().error(msg);
            }
        }
    }

    private PointCutClassTransformer startClassTransformer(InstrumentationProxy instrProxy) throws Exception {
        boolean retransformSupported = isRetransformationSupported(instrProxy);

        PointCutClassTransformer classTransformer = new PointCutClassTransformer(instrProxy, retransformSupported);
        contextManager = InstrumentationContextManager.create(classLoaderClassTransformer, instrProxy,
                AgentBridge.class.getClassLoader() == null);

        // Preload NR Transaction and related object to avoid ClassCircularity Error in Security instrumentation Module java-io-stream.
        NewRelic.getAgent().getTransaction();

        // Preload Security used classes to avoid complete application thread blocking in rare scenarios.
        ArrayUtils.isEmpty(new Object[0]);
        StringUtils.startsWithAny(StringUtils.LF, StringUtils.EMPTY, StringUtils.LF);
        new SecurityMetaData();
        ThreadLocalLockHelper.isLockHeldByCurrentThread();

        contextManager.addContextClassTransformer(classTransformer.getMatcher(), classTransformer);
        for (PointCut pc : classTransformer.getPointcuts()) {
            Agent.LOG.log(Level.FINER, "pointcut {0} active", pc);
            pc.noticeTransformerStarted(classTransformer);
        }
        Agent.LOG.log(Level.FINE, "enabled {0} pointcuts", classTransformer.getPointcuts().size());

        // create the class transformer prepopulated with the extensions loaded from disk
        localRetransformer = new ClassRetransformer(contextManager);
        localRetransformer.setClassMethodMatchers(ServiceFactory.getExtensionService().getEnabledPointCuts());

        // create the retransformer for instrumentation we receive from the remote instrumentation service
        remoteRetransformer = new ClassRetransformer(contextManager);

        traceMatchTransformer = new TraceMatchTransformer(contextManager);

        StartableClassFileTransformer[] startableClassTransformers = new StartableClassFileTransformer[] {
                // new NewRelicClassLoaderClassTransformer(classTransformer.getClassReaderFlags()),
                new InterfaceMixinClassTransformer(classTransformer.getClassReaderFlags())
                // new ClassLoaderClassTransformer(classTransformer.getClassReaderFlags())
        };
        for (StartableClassFileTransformer transformer : startableClassTransformers) {
            transformer.start(instrProxy, retransformSupported);
            classTransformers.add(transformer);
        }
        for (StartableClassFileTransformer transformer : InterfaceImplementationClassTransformer.getClassTransformers(classTransformer)) {
            transformer.start(instrProxy, retransformSupported);
            classTransformers.add(transformer);
        }
        return classTransformer;
    }

    private boolean isRetransformationSupported(InstrumentationProxy instrProxy) {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        Boolean enableClassRetransformation = config.getProperty(AgentConfigImpl.ENABLE_CLASS_RETRANSFORMATION);
        if (enableClassRetransformation != null) {
            return enableClassRetransformation;
        }
        try {
            return instrProxy.isRetransformClassesSupported();
        } catch (Exception e) {
            // AIX throws an UnsupportedOperationException
            String msg = MessageFormat.format(
                    "Unexpected error asking current JVM configuration if it supports retransformation of classes: {0}",
                    e);
            getLogger().warning(msg);
            return false;
        }
    }

    private void retransformMatchingClasses() {
        Set<ClassMatchVisitorFactory> matchers = retransformClassMatchers.getAndSet(createRetransformClassMatcherList());

        if (!matchers.isEmpty()) {
            InstrumentationProxy instrumentation = ServiceFactory.getCoreService().getInstrumentation();
            retransformMatchingClassesImmediately(instrumentation.getAllLoadedClasses(), matchers);
        }
    }

    @Override
    public void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> matchers) {
        retransformClassMatchers.get().addAll(matchers);
        queueRetransform();
    }

    @Override
    public void retransformMatchingClassesImmediately(Class<?>[] loadedClasses, Collection<ClassMatchVisitorFactory> matchers) {
        InstrumentationProxy instrumentation = ServiceFactory.getCoreService().getInstrumentation();
        InstrumentationContextClassMatcherHelper matcherHelper = new InstrumentationContextClassMatcherHelper();
        Set<Class<?>> classesToRetransform = ClassesMatcher.getMatchingClasses(matchers, matcherHelper, loadedClasses);
        if (!classesToRetransform.isEmpty()) {
            try {
                instrumentation.retransformClasses(classesToRetransform.toArray(new Class[0]));
            } catch (UnmodifiableClassException e) {
                logger.log(Level.FINER, "Error retransforming classes: " + classesToRetransform, e);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {

        executor.shutdown();

        InstrumentationProxy instrProxy = ServiceFactory.getCoreService().getInstrumentation();
        if (instrProxy == null) {
            return;
        }
        for (ClassFileTransformer classFileTransformer : classTransformers) {
            instrProxy.removeTransformer(classFileTransformer);
        }
    }

    @Override
    public InstrumentationContextManager getContextManager() {
        return contextManager;
    }

    @Override
    /**
     * For testing.
     */
    public PointCutClassTransformer getClassTransformer() {
        return classTransformer;
    }

    @Override
    public ClassRetransformer getLocalRetransformer() {
        return localRetransformer;
    }

    @Override
    public ClassRetransformer getRemoteRetransformer() {
        return remoteRetransformer;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix) {
        return this.addTraceMatcher(matcher, TraceDetailsBuilder.newBuilder().setMetricPrefix(metricPrefix).build());
    }

    @Override
    public boolean addTraceMatcher(ClassAndMethodMatcher matcher, TraceDetails traceDetails) {
        return traceMatchTransformer.addTraceMatcher(matcher, traceDetails);
    }

    @Override
    public Instrumentation getExtensionInstrumentation() {
        return extensionInstrumentation;
    }

    @Override
    public void retransformForAttach() {
        final AnnotationMatcher traceAnnotationMatcher = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getClassTransformerConfig().getTraceAnnotationMatcher();
        final Predicate<Class> traceMatcher = Utils.getAnnotationsMatcher(traceAnnotationMatcher);
        final List<Class> classesToRejit = Arrays.asList(getExtensionInstrumentation().getAllLoadedClasses())
                .stream().filter(traceMatcher)
                .collect(Collectors.toList());

        if (!classesToRejit.isEmpty()) {
            try {
                getExtensionInstrumentation().retransformClasses(classesToRejit.toArray(new Class[0]));
            } catch (UnmodifiableClassException e) {
                Agent.LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private static class TraceMatchTransformer implements ContextClassTransformer {

        private final Map<ClassAndMethodMatcher, TraceDetails> matchersToTraceDetails = new ConcurrentHashMap<>();
        private final Set<ClassMatchVisitorFactory> matchVisitors = Sets.newConcurrentHashSet();
        private final InstrumentationContextManager contextManager;

        TraceMatchTransformer(InstrumentationContextManager manager) {
            this.contextManager = manager;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match)
                throws IllegalClassFormatException {

            for (Method method : match.getMethods()) {
                for (ClassAndMethodMatcher matcher : match.getClassMatches().keySet()) {
                    if (matcher.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, method.getName(),
                            method.getDescriptor(), match.getMethodAnnotations(method))) {
                        context.putTraceAnnotation(method, matchersToTraceDetails.get(matcher));
                    }
                }
            }

            return null;
        }

        public boolean addTraceMatcher(ClassAndMethodMatcher matcher, TraceDetails traceDetails) {
            if (!matchersToTraceDetails.containsKey(matcher)) {
                return addMatchVisitor(matcher, traceDetails);
            }
            return false;
        }

        private synchronized boolean addMatchVisitor(ClassAndMethodMatcher matcher, TraceDetails traceDetails) {
            if (!matchersToTraceDetails.containsKey(matcher)) {
                matchersToTraceDetails.put(matcher, traceDetails);

                OptimizedClassMatcherBuilder builder = OptimizedClassMatcherBuilder.newBuilder();
                builder.addClassMethodMatcher(matcher);
                ClassMatchVisitorFactory matchVisitor = builder.build();
                matchVisitors.add(matchVisitor);
                contextManager.addContextClassTransformer(matchVisitor, this);

                return true;
            }
            return false;
        }
    }

}
