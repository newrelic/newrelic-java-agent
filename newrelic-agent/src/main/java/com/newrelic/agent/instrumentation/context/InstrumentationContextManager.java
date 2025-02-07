/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.instrumentation.ClassNameFilter;
import com.newrelic.agent.instrumentation.api.ApiImplementationUpdate;
import com.newrelic.agent.instrumentation.classmatchers.ScalaTraitMatcher;
import com.newrelic.agent.instrumentation.classmatchers.TraceLambdaVisitor;
import com.newrelic.agent.instrumentation.ejb3.EJBAnnotationVisitor;
import com.newrelic.agent.instrumentation.ejb4.EJB4AnnotationVisitor;
import com.newrelic.agent.instrumentation.otel.OtelInstrumentationService;
import com.newrelic.agent.instrumentation.tracing.TraceClassTransformer;
import com.newrelic.agent.instrumentation.weaver.ClassLoaderClassTransformer;
import com.newrelic.agent.instrumentation.weaver.ClassWeaverService;
import com.newrelic.agent.instrumentation.webservices.JakartaWebServiceVisitor;
import com.newrelic.agent.instrumentation.webservices.WebServiceVisitor;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.servlet.ServletAnnotationVisitor;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class InstrumentationContextManager {

    private static final String LOG4J_DEPENDENCY = "com/newrelic/agent/deps/org/apache/logging/log4j/";

    static final NoOpClassTransformer NO_OP_TRANSFORMER = new NoOpClassTransformer();

    private final Map<ClassMatchVisitorFactory, ContextClassTransformer> matchVisitors = new ConcurrentHashMap<>();
    private final Map<ClassMatchVisitorFactory, ContextClassTransformer> interfaceMatchVisitors = new ConcurrentHashMap<>();

    /**
     * A list of classloader class name prefixes. Any classloader class matching any of these prefixes will not have
     * its classes instrumented.
     */
    private final Set<String> classloaderExclusions;
    private final Instrumentation instrumentation;
    private final ClassWeaverService classWeaverService;
    private final OtelInstrumentationService otelInstrumentationService;

    /**
     * The {@link ClassFileTransformer} which is registered with the jvm.
     */
    private ClassFileTransformer jvmTransformer;

    /**
     * Include/Exclude rules for all Instrumentation Class Transformers.
     * This includes Weaver, PointCuts, annotation visitors, custom instrumentation, etc.
     */
    private final ClassNameFilter classNameFilter;

    public InstrumentationContextManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.classWeaverService = new ClassWeaverService(instrumentation);
        this.otelInstrumentationService = new OtelInstrumentationService();

        // these matchers only modify the InstrumentationContext, they don't actually transform classes
        matchVisitors.put(new ScalaTraitMatcher(), NO_OP_TRANSFORMER);
        matchVisitors.put(new TraceMatchVisitor(), NO_OP_TRANSFORMER);
        matchVisitors.put(new GeneratedClassDetector(), NO_OP_TRANSFORMER);

        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        ClassTransformerConfig classTransformerConfig = ServiceFactory.getConfigService()
                .getDefaultAgentConfig()
                .getClassTransformerConfig();

        if (agentConfig.getValue("instrumentation.web_services.enabled", false)) {
            Agent.LOG.log(Level.FINEST, "web_services instrumentation is enabled");
            matchVisitors.put(new WebServiceVisitor(), NO_OP_TRANSFORMER);
        } else if (!classTransformerConfig.isDefaultInstrumentationEnabled()) {
            Agent.LOG.log(Level.FINEST, "web_services instrumentation is disabled because it is not explicitly enabled");
        } else {
            matchVisitors.put(new WebServiceVisitor(), NO_OP_TRANSFORMER);
        }

        if (agentConfig.getValue("instrumentation.jakarta_web_services.enabled", false)) {
            Agent.LOG.log(Level.FINEST, "jakarta_web_services instrumentation is enabled");
            matchVisitors.put(new JakartaWebServiceVisitor(), NO_OP_TRANSFORMER);
        } else if (!classTransformerConfig.isDefaultInstrumentationEnabled()) {
            Agent.LOG.log(Level.FINEST, "jakarta_web_services instrumentation is disabled because it is not explicitly enabled");
        } else {
            matchVisitors.put(new JakartaWebServiceVisitor(), NO_OP_TRANSFORMER);
        }

        classNameFilter = new ClassNameFilter(Agent.LOG);
        classNameFilter.addConfigClassFilters(agentConfig);
        classNameFilter.addExcludeFileClassFilters();

        if (agentConfig.getValue("instrumentation.servlet_annotations.enabled", false)) {
            Agent.LOG.log(Level.FINEST, "servlet_annotations instrumentation is enabled");
            matchVisitors.put(new ServletAnnotationVisitor(), NO_OP_TRANSFORMER);
        } else if (!classTransformerConfig.isDefaultInstrumentationEnabled()) {
            Agent.LOG.log(Level.FINEST, "servlet_annotations instrumentation is disabled because it is not explicitly enabled");
        } else {
            matchVisitors.put(new ServletAnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        if (agentConfig.getValue("instrumentation.trace_lambda.enabled", false)) {
            Agent.LOG.log(Level.FINEST, "trace_lambda instrumentation is enabled");
            matchVisitors.put(new TraceLambdaVisitor(), NO_OP_TRANSFORMER);
        } else {
            Agent.LOG.log(Level.FINEST, "trace_lambda instrumentation is disabled because it is not explicitly enabled");
        }

        if (agentConfig.getValue("instrumentation.scala_future_trace.enabled", false)) {
          Agent.LOG.log(Level.FINEST, "scala_future_trace instrumentation is enabled");
          matchVisitors.put(new TraceByReturnTypeMatchVisitor(), NO_OP_TRANSFORMER);
        } else {
          Agent.LOG.log(Level.FINEST, "scala_future_trace instrumentation is disabled because it is not explicitly enabled");
        }

        Config ejb3InstrumentationConfig = agentConfig.getClassTransformerConfig().getInstrumentationConfig(
                "com.newrelic.instrumentation.ejb-3.0");
        if (ejb3InstrumentationConfig.getProperty("enabled", false)) {
            Agent.LOG.log(Level.FINEST, "ejb-3.0 instrumentation is enabled");
            matchVisitors.put(new EJBAnnotationVisitor(), NO_OP_TRANSFORMER);
        } else if (!classTransformerConfig.isDefaultInstrumentationEnabled()) {
            Agent.LOG.log(Level.FINEST, "ejb-3.0 instrumentation is disabled because it is not explicitly enabled");
        } else {
            matchVisitors.put(new EJBAnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        Config ejb4InstrumentationConfig = agentConfig.getClassTransformerConfig().getInstrumentationConfig(
                "com.newrelic.instrumentation.ejb-4.0");
        if (ejb4InstrumentationConfig.getProperty("enabled", false)) {
            Agent.LOG.log(Level.FINEST, "ejb-4.0 instrumentation is enabled");
            matchVisitors.put(new EJB4AnnotationVisitor(), NO_OP_TRANSFORMER);
        } else if (!classTransformerConfig.isDefaultInstrumentationEnabled()) {
            Agent.LOG.log(Level.FINEST, "ejb-4.0 instrumentation is disabled because it is not explicitly enabled");
        } else {
            matchVisitors.put(new EJB4AnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        classloaderExclusions = agentConfig.getClassTransformerConfig().getClassloaderExclusions();
        matchVisitors.put(ServiceFactory.getSourceLanguageService().getSourceVisitor(), NO_OP_TRANSFORMER);

        try {
            ApiImplementationUpdate.setup(this);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, e.toString());
        }
    }

    public Map<ClassMatchVisitorFactory, ContextClassTransformer> getMatchVisitors() {
        return matchVisitors;
    }

    public ClassWeaverService getClassWeaverService() {
        return classWeaverService;
    }

    public static InstrumentationContextManager create(final ClassLoaderClassTransformer classLoaderClassTransformer,
            final InstrumentationProxy instrumentation, final boolean bootstrapClassloaderEnabled) throws Exception {
        final InstrumentationContextManager manager = new InstrumentationContextManager(instrumentation);

        final TraceClassTransformer traceTransformer = new TraceClassTransformer();
        manager.classWeaverService.registerInstrumentation();
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        manager.classWeaverService.registerSecurityInstrumentation();
        manager.addContextClassTransformer(manager.classWeaverService, manager.classWeaverService);
        final boolean defaultMethodTracingEnabled = agentConfig.getClassTransformerConfig()
                .isDefaultMethodTracingEnabled();

        InstrumentationClassTransformer transformer = new InstrumentationClassTransformer(manager, traceTransformer,
                bootstrapClassloaderEnabled, defaultMethodTracingEnabled);
        instrumentation.addTransformer(transformer, true);
        manager.jvmTransformer = transformer;

        // We don't want to use an OptimizedClassMatcher as the ClassMatchVisitorFactory here (the first parameter)
        // because it uses ClassLoader.findResource() internally and this opens us up to the possibility of deadlocks.
        // So instead, we will add this class as a matchVisitor that matches based on the observedClassLoaders map.
        manager.addContextClassTransformer(classLoaderClassTransformer, classLoaderClassTransformer);

        // Remove the temporary transformer we applied during startup
        instrumentation.removeTransformer(classLoaderClassTransformer);

        manager.classWeaverService.createRetransformRunnable(instrumentation.getAllLoadedClasses()).run();
        transformer.setInitialized(true);

        return manager;
    }

    /**
     * Visit the reader representing an interface with all of the {@link #interfaceMatchVisitors}.
     */
    protected void applyInterfaceVisitors(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader) {
        ClassVisitor cv = null;
        for (ClassMatchVisitorFactory factory : interfaceMatchVisitors.keySet()) {
            cv = factory.newClassMatchVisitor(loader, classBeingRedefined, reader, cv, null);
        }
        if (cv != null) {
            reader.accept(cv, ClassReader.SKIP_CODE);
        }
    }

    public boolean shouldTransform(final String internalClassName, final ClassLoader classloader) {
        if (null == internalClassName) {
            return false;
        }
        if (internalClassName.startsWith(LOG4J_DEPENDENCY)) {
            // Do not log anything when skipping log4j classes. This prevents a "recursive call to appender" message from log4j
            return false;
        }
        if (isClassloaderExcluded(classloader)) {
            Agent.LOG.log(Level.FINEST, "Skipping transform of {0}. Classloader {1} is excluded.", internalClassName, classloader);
            return false;
        }
        if (classNameFilter.isIncluded(internalClassName)) {
            Agent.LOG.log(Level.FINEST, "Class {0} is explicitly included", internalClassName);
            return true;
        }
        if (classNameFilter.isExcluded(internalClassName)) {
            Agent.LOG.log(Level.FINEST, "Skipping class {0} because it is excluded", internalClassName);
            return false;
        }

        return true;
    }

    /**
     * Classes loaded by the classloaders called out in the ClassLoader Excludes List will not be instrumented.
     */
    public boolean isClassloaderExcluded(final ClassLoader classloader) {
        final String clName;
        if (null == classloader) {
            clName = WeaveUtils.BOOTSTRAP_PLACEHOLDER.getClass().getName();
        } else {
            clName = classloader.getClass().getName();
        }
        for (String excludedEntry : classloaderExclusions) {
            if (clName.startsWith(excludedEntry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a factory that generates class visitors to match classes and the class transformer that is responsible for
     * processing successful matches.
     */
    public void addContextClassTransformer(ClassMatchVisitorFactory matchVisitor, ContextClassTransformer transformer) {
        if (transformer == null) {
            transformer = NO_OP_TRANSFORMER;
        }
        this.matchVisitors.put(matchVisitor, transformer);
    }

    /**
     * Remove a class match visitor factory.
     *
     * @param visitor
     */
    public void removeMatchVisitor(ClassMatchVisitorFactory visitor) {
        this.matchVisitors.remove(visitor);
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * Get the {@link ClassFileTransformer} registered with the jvm. Only use for testing.
     */
    public ClassFileTransformer getJvmClassTransformer() {
        return this.jvmTransformer;
    }

}
