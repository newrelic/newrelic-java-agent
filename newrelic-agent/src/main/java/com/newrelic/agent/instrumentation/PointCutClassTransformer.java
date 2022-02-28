/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.util.Annotations;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationHandler;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * Class transformer for legacy instrumentation.
 *
 * Instrumentation points are defined through {@link PointCut}s.
 *
 * Certain classes are always skipped based on their full class name.
 *
 * @see ClassNameFilter
 */
public class PointCutClassTransformer implements ContextClassTransformer {
    /**
     * Exclude logic has been moved to the weaver and
     * {@link com.newrelic.agent.instrumentation.context.InstrumentationContextManager}. Still, some additional rules
     * are added here to preserve the legacy behavior of the pointcut matchers. These rules only apply to this pointcut
     * transformer.
     */
    private static final String[] LEGACY_POINTCUT_EXCLUDES = {
        "^(java/|sun/|com/sun/|com/newrelic/agent/|com/newrelic/org/)(.*)",
        "^org/apache/catalina/startup/Bootstrap",
        "^com/ibm/ws/webcontainer/servlet/ServletWrapper"};

    protected final Collection<PointCut> pointcuts;
    private final int classreaderFlags;

    private final InstrumentationProxy instrumentation;
    private final boolean retransformSupported;

    /**
     * Include/exclude logic specific to the pointcut transformer. Some of the rules are duplicated from
     * InstrumentationContextManager to preserve old pointcut rules.
     */
    private final ClassNameFilter classNameFilter;
    private final IAgentLogger logger;
    private final ClassMatchVisitorFactory matcher;

    protected PointCutClassTransformer(InstrumentationProxy pInstrumentation, boolean pRetransformSupported) {
        instrumentation = pInstrumentation;
        logger = Agent.LOG.getChildLogger(PointCutClassTransformer.class);
        initAgentHandle();
        classNameFilter = new ClassNameFilter(logger);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        classNameFilter.addConfigClassFilters(config);
        classNameFilter.addExcludeFileClassFilters();
        for (String legacyPointCutExclude : LEGACY_POINTCUT_EXCLUDES) {
            classNameFilter.addExclude(legacyPointCutExclude);
        }

        classreaderFlags = instrumentation.getClassReaderFlags();
        retransformSupported = pRetransformSupported;

        List<PointCut> pcs = new LinkedList<>(findEnabledPointCuts());
        pcs.addAll(ErrorServiceImpl.getEnabledErrorHandlerPointCuts());

        Collections.sort(pcs);
        pointcuts = Collections.unmodifiableCollection(pcs);

        setPointcutProperties();
        matcher = OptimizedClassMatcherBuilder.newBuilder().addClassMethodMatcher(pointcuts.toArray(new PointCut[0])).build();

    }

    public ClassMatchVisitorFactory getMatcher() {
        return matcher;
    }

    private void setPointcutProperties() {

        // iterate over list once
        List<PointCutInvocationHandler> handlers = new ArrayList<>(pointcuts.size());
        Collection<ClassMatcher> classMatchers = new ArrayList<>();
        for (PointCut pc : pointcuts) {
            handlers.add(pc.getPointCutInvocationHandler());
            classMatchers.add(pc.getClassMatcher());
        }
        classNameFilter.addClassMatcherIncludes(classMatchers);
        ServiceFactory.getTracerService().registerInvocationHandlers(handlers);

        logger.finer("A Class transformer is initialized");
    }

    /**
     * Initialize {@link AgentBridge#agentHandler}.
     */
    private void initAgentHandle() {
        AgentBridge.agentHandler = AgentWrapper.getAgentWrapper(this);
    }

    Collection<PointCut> findEnabledPointCuts() {
        Collection<Class<?>> classes = new Annotations().getPointCutAnnotatedClasses();
        Collection<PointCut> pointcuts = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (PointCut.class.isAssignableFrom(clazz)) {
                PointCut pc = createPointCut(clazz.asSubclass(PointCut.class));
                if (pc != null && pc.isEnabled()) {
                    pointcuts.add(pc);
                }
            }
        }

        return pointcuts;
    }

    private PointCut createPointCut(Class<? extends PointCut> clazz) {
        try {
            return clazz.getConstructor(PointCutClassTransformer.class).newInstance(this);
        } catch (Exception e) {
            final String msg = MessageFormat.format("Unable to create pointcut {0} : {1}", clazz.getName(),
                    e.toString());
            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINE, msg, e);
        }
        return null;
    }

    public Collection<PointCut> getPointcuts() {
        return pointcuts;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match)
            throws IllegalClassFormatException {

        if (!isValidClassName(className)) {
            return null;
        }

        if (!shouldTransform(loader, className, classfileBuffer)) {
            logger.trace(MessageFormat.format("PointCutTransformer Skipped instrumenting {0}", className));
            return null;
        }

        try {
            WeavingLoaderImpl weavingLoader = getWeavingLoader(loader);
            if (Agent.isDebugEnabled() && logger.isTraceEnabled()) {
                logger.trace(MessageFormat.format("Considering instrumenting {0}", className));
            }
            return weavingLoader.preProcess(context, className, classBeingRedefined, classfileBuffer, match);
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable e) {
            logger.severe(MessageFormat.format("An error occurred processing class {0} : {1}", className, e.toString()));
            if (Agent.isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    protected boolean shouldTransform(ClassLoader loader, String className, byte[] classfileBuffer) {

        final boolean isLoggable = Agent.isDebugEnabled() && logger.isLoggable(Level.FINEST);

        if (isIncluded(className)) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("PointCutTransformer Class {0} is explicitly included", className));
            }
            return true;
        }

        if (isExcluded(className)) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("PointCutTransformer Skipping class {0} because it is excluded", className));
            }
            return false;
        }

        if (className.startsWith("$")) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("PointCutTransformer Skipping class {0} because it starts with $", className));
            }
            return false;
        }

        // Play2 has a lot of classes with $$.
        if ((className.indexOf("$$") > 0) && (!className.startsWith("play"))) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("PointCutTransformer Skipping class {0} because it contains $$ and is not a Play class",
                        className));
            }
            return false;
        }

        if (isValidClassByteArray(classfileBuffer)) {
            if (isLoggable) {
                logger.finest(MessageFormat.format(
                        "PointCutTransformer Skipping class {0} because it does not appear to be a valid class file", className));
            }
            return false;
        }

        if (loader == null && !isBootstrapClassInstrumentationEnabled()) {
            if (isLoggable) {
                logger.finest(MessageFormat.format(
                        "PointCutTransformer Skipping class {0} because bootstrap class instrumentation is not supported", className));
            }
            return false;
        }

        return true;
    }

    private boolean isBootstrapClassInstrumentationEnabled() {
        return instrumentation.isBootstrapClassInstrumentationEnabled();
    }

    protected boolean isIncluded(String className) {
        return classNameFilter.isIncluded(className);
    }

    protected boolean isExcluded(String className) {
        return classNameFilter.isExcluded(className);
    }

    /**
     * Gets the field retransformSupported.
     * 
     * @return the retransformSupported
     */
    protected boolean isRetransformSupported() {
        return retransformSupported;
    }

    /**
     * Returns true if the given byte array looks like a class.
     * 
     * @param classfileBuffer
     */
    private boolean isValidClassByteArray(byte[] classfileBuffer) {
        return classfileBuffer.length >= 4 && classfileBuffer[0] == (byte) 0xCA && classfileBuffer[0] == (byte) 0xFE
                && classfileBuffer[0] == (byte) 0xBA && classfileBuffer[0] == (byte) 0xBE;
    }

    protected int getClassReaderFlags() {
        return classreaderFlags;
    }

    protected WeavingLoaderImpl getWeavingLoader(ClassLoader loader) {
        return new WeavingLoaderImpl(loader);
    }

    protected WeavingLoaderImpl getWeavingLoader(ClassLoader loader, boolean pIsRetrans) {
        return new WeavingLoaderImpl(loader);
    }

    class WeavingLoaderImpl {
        private final ClassLoader classLoader;

        public WeavingLoaderImpl(ClassLoader classLoader) {
            super();
            this.classLoader = classLoader;
        }

        public byte[] preProcess(InstrumentationContext context, final String className, Class<?> classBeingRedefined,
                final byte[] classfileBuffer, Match match) {

            ClassReader cr = new ClassReader(classfileBuffer);
            if (InstrumentationUtils.isInterface(cr)) { // skip interfaces since they have no implementation code
                return null;
            }

            Collection<PointCut> strongMatches = new ArrayList<>(pointcuts);
            strongMatches.retainAll(match.getClassMatches().keySet());

            if (strongMatches.isEmpty()) {
                return null;
            }

            if (classLoader != null && !InstrumentationUtils.isAbleToResolveAgent(classLoader, className)) {
                String msg = MessageFormat.format("Not instrumenting {0}: class loader unable to load agent classes",
                        className);
                Agent.LOG.log(Level.FINER, msg);
                return null;
            }

            try {
                if (canModifyClassStructure(classLoader, classBeingRedefined)) {
                    byte[] classfileBufferWithUID = InstrumentationUtils.generateClassBytesWithSerialVersionUID(cr,
                            classreaderFlags, classLoader);
                    cr = new ClassReader(classfileBufferWithUID);
                }

                ClassWriter cw = InstrumentationUtils.getClassWriter(cr, classLoader);

                GenericClassAdapter adapter = new GenericClassAdapter(cw, classLoader, className, classBeingRedefined,
                        strongMatches, context);
                cr.accept(adapter, classreaderFlags);

                if (adapter.getInstrumentedMethods().size() > 0) {
                    /*
                     * ClassReader cr2 = new ClassReader(cw.toByteArray()); ClassWriter cw2 = new ClassWriter(cr2,
                     * ClassWriter.COMPUTE_MAXS); CheckClassAdapter cc = new CheckClassAdapter(cw2); cr2.accept(cc,
                     * classreaderFlags); StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw);
                     * CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, pw);
                     * 
                     * if (sw.toString().length() > 0) { System.err.println(sw.toString()); }
                     */
                    if (Agent.LOG.isFinerEnabled()) {
                        final String msg = MessageFormat.format("Instrumenting {0}", className);
                        Agent.LOG.finer(msg);
                    }

                    recordSupportabilityMetrics(adapter.getAppliedPointCuts());
                    return cw.toByteArray();
                } else {
                    // class isn't interesting
                    return null;
                }
            } catch (StopProcessingException e) {
                // ignore
                return null;
            } catch (ArrayIndexOutOfBoundsException t) {
                String msg = MessageFormat.format(
                        "Skipping transformation of class {0} ({1} bytes) because an ASM array bounds exception occurred: {2}",
                        className, classfileBuffer.length, t.toString());
                logger.warning(msg);
                if (logger.isLoggable(Level.FINER)) {
                    msg = MessageFormat.format("ASM error for pointcut(s) : strong {0}", strongMatches);
                    logger.finer(msg);
                    logger.log(Level.FINER, "ASM error", t);
                }
                if (Boolean.getBoolean("newrelic.asm.error.stop")) {
                    System.exit(-1);
                }
                return null;
            } catch (ThreadDeath e) {
                throw e;
            } catch (Throwable t) {
                logger.warning(MessageFormat.format(
                        "Skipping transformation of class {0} because an error occurred: {1}", className, t.toString()));
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Error transforming class " + className, t);
                }
                return null;
            }
        }

        private void recordSupportabilityMetrics(List<PointCut> appliedPointCuts) {
            for (PointCut pointCut: appliedPointCuts) {
                String pointCutMetricName = MessageFormat.format(
                        MetricNames.SUPPORTABILITY_POINTCUT_LOADED, pointCut.getName());
                ServiceFactory.getStatsService().doStatsWork(StatsWorks.getRecordMetricWork(pointCutMetricName, 1), pointCutMetricName);
            }
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

    }

    InstrumentationProxy getInstrumentation() {
        return instrumentation;
    }

    public final ClassNameFilter getClassNameFilter() {
        return classNameFilter;
    }

    public InvocationHandler evaluate(Class clazz, TracerService tracerService, Object className, Object methodName,
            Object methodDesc, boolean ignoreApdex, Object[] args) {
        ClassMethodSignature classMethodSignature = new ClassMethodSignature(((String) className).replace('/', '.'),
                (String) methodName, (String) methodDesc);

        for (PointCut pc : getPointcuts()) {
            // Hey, that's right! Any method matcher that wants to use the access value is not going to work with this
            // code. Bummer for you!
            if (pc.getClassMatcher().isMatch(clazz)
                    && pc.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS,
                            classMethodSignature.getMethodName(), classMethodSignature.getMethodDesc(),
                            MethodMatcher.UNSPECIFIED_ANNOTATIONS)) {
                PointCutInvocationHandler invocationHandler = pc.getPointCutInvocationHandler();
                return InvocationPoint.getInvocationPoint(invocationHandler, tracerService, classMethodSignature,
                        ignoreApdex);
            }
        }
        if (ignoreApdex) {
            return IgnoreApdexInvocationHandler.INVOCATION_HANDLER;
        }

        Agent.LOG.log(Level.FINE, "No invocation handler was registered for {0}", classMethodSignature);

        return NoOpInvocationHandler.INVOCATION_HANDLER;
    }

    public static boolean isInstrumented(Class<?> clazz) {
        if (clazz.getAnnotation(InstrumentedClass.class) != null) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isInstrumentedAndModified(Class<?> clazz) {
        if (clazz.getAnnotation(InstrumentedClass.class) != null) {
            return clazz.getAnnotation(InstrumentedClass.class).classStructureModified();
        }
        return false;
    }

    public static boolean canModifyClassStructure(ClassLoader classLoader, Class<?> classBeingRedefined) {
        if (!hasBeenLoaded(classBeingRedefined) || isInstrumentedAndModified(classBeingRedefined)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean hasBeenLoaded(Class<?> clazz) {
        return null != clazz;
    }

    public static boolean isValidClassName(String className) {
        // className might be null for lambdas so we don't want to transform them
        return className != null;
    }

}
