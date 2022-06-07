/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.tracing.TraceClassVisitor;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.util.asm.ClassResolver;
import com.newrelic.agent.util.asm.ClassResolvers;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class tracks information about a class passing through the {@link InstrumentationContextManager}. It keeps track
 * of the methods of the class that have been matched by different class transformers that are registered with the
 * manager.
 */
public class InstrumentationContext implements TraceDetailsList {

    private static final TraceInformation EMPTY_TRACE_INFO = new TraceInformation();

    protected final byte[] bytes;
    private boolean modified;
    private Multimap<Method, String> weavedMethods;
    private Multimap<Method, String> skipMethods;
    private Set<String> nonFinalFields;
    private Set<Method> timedMethods;
    private Map<Method, PointCut> oldReflectionStyleInstrumentationMethods;
    private Map<Method, PointCut> oldInvokerStyleInstrumentationMethods;
    private TraceInformation tracedInfo;
    private Map<ClassMatchVisitorFactory, Match> matches;
    private String[] interfaces;
    private Map<Method, Method> bridgeMethods;
    private String className;
    private final Class<?> classBeingRedefined;
    private final ProtectionDomain protectionDomain;
    private List<ClassResolver> classResolvers;
    private boolean generated;
    private boolean hasSource;

    public InstrumentationContext(byte[] bytes, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
        this.bytes = bytes;
        this.classBeingRedefined = classBeingRedefined;
        this.protectionDomain = protectionDomain;
    }

    public String[] getInterfaces() {
        return null == interfaces ? new String[0] : interfaces;
    }

    public String getClassName() {
        return className;
    }

    public Class<?> getClassBeingRedefined() {
        return classBeingRedefined;
    }

    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    public void markAsModified() {
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public TraceInformation getTraceInformation() {
        return tracedInfo == null ? EMPTY_TRACE_INFO : tracedInfo;
    }

    public boolean isTracerMatch() {
        return (tracedInfo != null && tracedInfo.isMatch());
    }

    /**
     * Adds a weaved method.
     *
     * @param instrumentationTitle The name of the instrumentation package from which the weaved code originated.
     */
    public void addWeavedMethod(Method method, String instrumentationTitle) {
        if (weavedMethods == null) {
            weavedMethods = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        }
        if(skipMethods== null || !skipMethods.containsKey(method)) {
          weavedMethods.put(method, instrumentationTitle);
          modified = true;
        }
    }

    public void addSkipMethod(Method method, String owningClass) {
      if (skipMethods == null) {
        skipMethods = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
      }
      skipMethods.put(method, owningClass);
    }

    public void addNonFinalFields(String fieldName) {
      if (nonFinalFields == null) {
        nonFinalFields = new HashSet<>();
      }
      nonFinalFields.add(fieldName);
    }

    public PointCut getOldStylePointCut(Method method) {
        PointCut pc = getOldInvokerStyleInstrumentationMethods().get(method);
        if (pc == null) {
            pc = getOldReflectionStyleInstrumentationMethods().get(method);
        }
        return pc;
    }

    private Map<Method, PointCut> getOldInvokerStyleInstrumentationMethods() {
        return oldInvokerStyleInstrumentationMethods == null ? Collections.<Method, PointCut>emptyMap()
                : oldInvokerStyleInstrumentationMethods;
    }

    private Map<Method, PointCut> getOldReflectionStyleInstrumentationMethods() {
        return oldReflectionStyleInstrumentationMethods == null ? Collections.<Method, PointCut>emptyMap()
                : oldReflectionStyleInstrumentationMethods;
    }

    public Set<Method> getWeavedMethods() {
        return weavedMethods == null ? Collections.<Method>emptySet() : weavedMethods.keySet();
    }

  public Map<Method, Collection<String>> getSkipMethods() {
    return skipMethods == null ? Collections.emptyMap() : skipMethods.asMap();
  }

  public Set<String> getNonFinalFields() {
      return nonFinalFields == null ? Collections.emptySet() : new HashSet<>(nonFinalFields);
  }

  /**
     * Returns methods that are timed with instrumentation injected by the new {@link TraceClassVisitor} or the old
     * GenericClassAdapter.
     */
    public Set<Method> getTimedMethods() {
        return timedMethods == null ? Collections.<Method>emptySet() : timedMethods;
    }

    public Collection<String> getMergeInstrumentationPackages(Method method) {
        return weavedMethods == null ? Collections.<String>emptySet() : weavedMethods.asMap().get(method);
    }

    public boolean isModified(Method method) {
        return (getTimedMethods().contains(method)) || (getWeavedMethods().contains(method));
    }

    /**
     * Adds methods that are timed with method tracers.
     */
    public void addTimedMethods(Method... methods) {
        if (timedMethods == null) {
            timedMethods = new HashSet<>();
        }
        Collections.addAll(timedMethods, methods);
        modified = true;
    }

    public void addOldReflectionStyleInstrumentationMethod(Method method, PointCut pointCut) {
        if (oldReflectionStyleInstrumentationMethods == null) {
            oldReflectionStyleInstrumentationMethods = new HashMap<>();
        }
        oldReflectionStyleInstrumentationMethods.put(method, pointCut);
        modified = true;
    }

    public void addOldInvokerStyleInstrumentationMethod(Method method, PointCut pointCut) {
        if (oldInvokerStyleInstrumentationMethods == null) {
            oldInvokerStyleInstrumentationMethods = new HashMap<>();
        }
        oldInvokerStyleInstrumentationMethods.put(method, pointCut);
        modified = true;
    }

    public Map<ClassMatchVisitorFactory, Match> getMatches() {
        return matches == null ? Collections.<ClassMatchVisitorFactory, Match>emptyMap() : matches;
    }

    public void putTraceAnnotation(Method method, TraceDetails traceDetails) {
        if (tracedInfo == null) {
            tracedInfo = new TraceInformation();
        }
        tracedInfo.putTraceAnnotation(method, traceDetails);
    }

    public void addIgnoreApdexMethod(String methodName, String methodDesc) {
        if (tracedInfo == null) {
            tracedInfo = new TraceInformation();
        }
        tracedInfo.addIgnoreApdexMethod(methodName, methodDesc);
    }

    public void addIgnoreTransactionMethod(String methodName, String methodDesc) {
        if (tracedInfo == null) {
            tracedInfo = new TraceInformation();
        }
        tracedInfo.addIgnoreTransactionMethod(methodName, methodDesc);
    }

    public void addIgnoreTransactionMethod(Method m) {
        if (tracedInfo == null) {
            tracedInfo = new TraceInformation();
        }
        tracedInfo.addIgnoreTransactionMethod(m);
    }

    public void putMatch(ClassMatchVisitorFactory matcher, Match match) {
        if (matches == null) {
            matches = new HashMap<>();
        }
        matches.put(matcher, match);
    }

    public void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Adds methods to be traced (timed) by instrumentation injected by the {@link TraceClassVisitor}.
     */
    public void addTracedMethods(Map<Method, TraceDetails> tracedMethods) {
        if (tracedInfo == null) {
            tracedInfo = new TraceInformation();
        }
        tracedInfo.pullAll(tracedMethods);
    }

    /**
     * Adds a method to be traced (timed) by instrumentation injected by the {@link TraceClassVisitor}.
     */
    @Override
    public void addTrace(Method method, TraceDetails traceDetails) {
        if (tracedInfo == null) {
            tracedInfo = new TraceInformation();
        }
        tracedInfo.putTraceAnnotation(method, traceDetails);
    }

    public void match(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
            Collection<ClassMatchVisitorFactory> classVisitorFactories) {

        ClassVisitor visitor = null;
        for (ClassMatchVisitorFactory factory : classVisitorFactories) {
            ClassVisitor nextVisitor = factory.newClassMatchVisitor(loader, classBeingRedefined, reader, visitor, this);
            if (nextVisitor != null) {
                visitor = nextVisitor;
            }
        }
        if (visitor != null) {
            reader.accept(visitor, ClassReader.SKIP_FRAMES);
            if (bridgeMethods != null) {
                // resolve bridge methods
                resolveBridgeMethods(reader);

            } else {
                bridgeMethods = ImmutableMap.of();
            }
        }
    }

    /**
     * {@link ClassMatchVisitorFactory} implementations add bridge methods that they've matched to the
     * {@link #bridgeMethods} map. In that initial pass they just add the method but don't resolve the actual
     * implementation. In a second pass we visit the code to resolve the signature of the actual implementation.
     *
     * For example, if a class implements the generic {@link List} interface and specifies that the type is
     * {@link Integer}, the matchers will add the add(Object) method to our bridged method map, and this method will set
     * the value to the add(Integer) method which implements the add method.
     *
     * @see Opcodes#ACC_BRIDGE
     */
    private void resolveBridgeMethods(ClassReader reader) {
        ClassVisitor visitor = new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                final Method method = new Method(name, desc);
                if (bridgeMethods.containsKey(method)) {
                    return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            bridgeMethods.put(method, new Method(name, desc));
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                    };

                }
                return null;
            }

        };

        reader.accept(visitor, ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES);
    }


    /**
     * Add a bridged method to this context.
     *
     * @see Opcodes#ACC_BRIDGE
     */
    public void addBridgeMethod(Method method) {
        if (bridgeMethods == null) {
            bridgeMethods = new HashMap<>();
        }
        bridgeMethods.put(method, method);
    }

    /**
     * Returns a map of bridge methods. The key is the generic method definition from the matchers, and the value is the
     * actual method implementation with generic types. For example, a class may implement List<T> with a specific type
     * of Person. The class will implement a method called add(Person) and the JVM will generate the bridge method
     * add(Object) which will simply invoke add(Person). Generally speaking, we want to be able to create matchers that
     * match the loosely typed version of the method (the signature that's usually a bridge method), but we want to
     * instrument the typed version of the method because it can be invoked directly without passing through the bridge
     * implementation.
     */
    public Map<Method, Method> getBridgeMethods() {
        return bridgeMethods;
    }

    public boolean isUsingLegacyInstrumentation() {
        return null != oldInvokerStyleInstrumentationMethods || null != oldReflectionStyleInstrumentationMethods;
    }

    public boolean hasModifiedClassStructure() {
        return null != oldInvokerStyleInstrumentationMethods;
    }

    /**
     * Adds a class resolver to the current context.
     */
    public void addClassResolver(ClassResolver classResolver) {
        if (this.classResolvers == null) {
            this.classResolvers = new ArrayList<>();
        }
        this.classResolvers.add(classResolver);
    }

    /**
     * Returns a class resolver that will delegate to the class resolvers added with
     * {@link #addClassResolver(ClassResolver)}. If those fail to resolve the class the given classloader is used.
     *
     * @see ClassResolvers#getClassLoaderResolver(ClassLoader)
     */
    public ClassResolver getClassResolver(ClassLoader loader) {
        ClassResolver classResolver = ClassResolvers.getClassLoaderResolver(loader);
        if (classResolvers != null) {
            classResolvers.add(classResolver);
            classResolver = ClassResolvers.getMultiResolver(classResolvers);
        }
        return classResolver;
    }

    public void setGenerated(boolean isGenerated) {
        this.generated = isGenerated;
    }

    /**
     * Return true if the GeneratedClassDetector identified this class as a generated class.
     */
    public boolean isGenerated() {
        return generated;
    }

    public void setSourceAttribute(boolean hasSource) {
        this.hasSource = hasSource;
    }

    /**
     * Return true if the GeneratedClassDetector found that this class has a source attribute. Java class files are not
     * required to have a source attribute. When a class is created by a compiler, the source attribute generally
     * contains the name of the source file. When a class is generated by a bytecode tool, the attribute may contain
     * anything or may be absent.
     *
     * @return true if a source attribute was found on the class file.
     */
    public boolean hasSourceAttribute() {
        return hasSource;
    }

    public URL getCodeSourceLocation(){
        if((protectionDomain == null) || (protectionDomain.getCodeSource() == null)) {
            return null;
        }
        return protectionDomain.getCodeSource().getLocation();
    }
}
