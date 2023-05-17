/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactParamsMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * {@link OptimizedClassMatcher}s are threadsafe.
 */
public final class OptimizedClassMatcher implements ClassMatchVisitorFactory {

    public static final Set<Method> METHODS_WE_NEVER_INSTRUMENT = ImmutableSet.of(new Method("equals",
            "(Ljava/lang/Object;)Z"), new Method("toString", "()Ljava/lang/String;"), new Method("finalize", "()V"),
            new Method("hashCode", "()I"));

    static final OptimizedClassMatcher EMPTY_MATCHER = new OptimizedClassMatcher();

    /**
     * An array of MethodMatchers to ClassAndMethodMatchers. This will contain method matchers which loosely match
     * methods, like the {@link AllMethodsMatcher} or {@link ExactParamsMethodMatcher}. We use an array here instead of
     * a collection so we don't have to create iterators to loop through the list.
     */
    final Entry<MethodMatcher, ClassAndMethodMatcher>[] methodMatchers;

    /**
     * A map of exact methods to match to the ClassAndMethodMatchers which match them.
     */
    final Map<Method, Collection<ClassAndMethodMatcher>> methods;

    /**
     * A set of method annotation descriptors to match.
     */
    final Set<String> methodAnnotationsToMatch;

    Set<String> exactClassNames;

    public static final Method DEFAULT_CONSTRUCTOR = new Method("<init>", "()V");

    @SuppressWarnings("unchecked")
    private OptimizedClassMatcher() {
        this.methodAnnotationsToMatch = ImmutableSet.of();
        this.methodMatchers = new Entry[0];
        this.methods = ImmutableMap.of();
    }

    @SuppressWarnings("unchecked")
    protected OptimizedClassMatcher(Set<String> annotationMatchers, SetMultimap<Method, ClassAndMethodMatcher> methods,
            SetMultimap<MethodMatcher, ClassAndMethodMatcher> methodMatchers, Set<String> exactClassNames) {
        this.methodAnnotationsToMatch = ImmutableSet.copyOf(annotationMatchers);
        this.methodMatchers = methodMatchers.entries().toArray(new Entry[0]);

        this.methods = ImmutableMap.copyOf(methods.asMap());
        this.exactClassNames = exactClassNames == null ? null : ImmutableSet.copyOf(exactClassNames);
    }

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
            ClassVisitor cv, InstrumentationContext context) {
        if (exactClassNames != null && !exactClassNames.contains(reader.getClassName())) {
            return null;
        }
        return new ClassMethods(loader, reader, classBeingRedefined, cv, context);
    }

    static final Supplier<Set<String>> STRING_COLLECTION_SUPPLIER = HashSet::new;

    private Multimap<ClassAndMethodMatcher, String> newClassMatches() {

        return Multimaps.newSetMultimap(new HashMap<ClassAndMethodMatcher, Collection<String>>(),
                STRING_COLLECTION_SUPPLIER);
    }

    public static final class Match {

        public static final Match NOOP_MATCH = new Match(ImmutableMultimap.<ClassAndMethodMatcher, String> of(),
                Collections.<Method>emptySet(), Collections.<Method, Set<String>>emptyMap());

        private final Map<ClassAndMethodMatcher, Collection<String>> classNames;
        private final Set<Method> methods;
        private final Map<Method, Set<String>> methodAnnotations;

        public Match(Multimap<ClassAndMethodMatcher, String> classMatches, Set<Method> methods,
                Map<Method, Set<String>> methodAnnotations) {
            this.classNames = ImmutableMap.copyOf(classMatches.asMap());
            this.methods = ImmutableSet.copyOf(methods);
            this.methodAnnotations = methodAnnotations == null ? ImmutableMap.<Method, Set<String>> of()
                    : methodAnnotations;
        }

        /**
         * A map of the ClassAndMethodMatchers that matched to the class names which they matched.
         *
         */
        public Map<ClassAndMethodMatcher, Collection<String>> getClassMatches() {
            return classNames;
        }

        /**
         * A set of the methods that were matched.
         *
         */
        public Set<Method> getMethods() {
            return methods;
        }

        /**
         * Returns the annotations for a method. Note that this only returns annotations that were matched by
         * {@link AnnotationMethodMatcher}s belonging to the owning {@link OptimizedClassMatcher}.
         *
         * @param method
         */
        public Set<String> getMethodAnnotations(Method method) {
            Set<String> set = methodAnnotations.get(method);
            return set == null ? ImmutableSet.<String> of() : set;
        }

        public boolean isClassAndMethodMatch() {
            return !(methods.isEmpty() || classNames.isEmpty());
        }

        @Override
        public String toString() {
            return classNames.toString() + " methods " + methods;
        }

    }

    private class ClassMethods extends ClassVisitor {

        private final Class<?> classBeingRedefined;
        private final ClassReader cr;
        private final ClassLoader loader;
        private SetMultimap<Method, ClassAndMethodMatcher> matches;
        private Map<Method, Set<String>> methodAnnotations;

        /**
         * Cache the ClassMatcher match results because these can be expensive to compute.
         */
        private Map<ClassMatcher, Boolean> classMatcherMatches;
        private final InstrumentationContext context;

        private ClassMethods(ClassLoader loader, ClassReader cr, Class<?> classBeingRedefined, ClassVisitor cv,
                InstrumentationContext context) {
            super(WeaveUtils.ASM_API_LEVEL, cv);

            this.cr = cr;
            this.classBeingRedefined = classBeingRedefined;
            this.loader = loader;
            this.context = context;
        }

        private void addMethodAnnotations(Method method, Set<String> annotations) {
            if (!annotations.isEmpty()) {
                if (methodAnnotations == null) {
                    methodAnnotations = new HashMap<>();
                }
                methodAnnotations.put(method, annotations);
            }
        }

        private SetMultimap<Method, ClassAndMethodMatcher> getOrCreateMatches() {
            if (matches == null) {
                matches = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
            }
            return matches;
        }

        private boolean isMatch(ClassMatcher classMatcher, ClassLoader loader, ClassReader cr,
                Class<?> classBeingRedefined) {
            if (null == classMatcherMatches) {
                classMatcherMatches = new HashMap<>();
            }
            Boolean match = classMatcherMatches.get(classMatcher);
            if (match == null) {
                match = classBeingRedefined == null ? classMatcher.isMatch(loader, cr)
                        : classMatcher.isMatch(classBeingRedefined);
                classMatcherMatches.put(classMatcher, match);
            }

            return (match);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String methodName, final String methodDesc,
                String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);

            if ((access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_NATIVE) == 0) {
                final Method method = new Method(methodName, methodDesc);
                if (METHODS_WE_NEVER_INSTRUMENT.contains(method)) {
                    return mv;
                }

                if (!methodAnnotationsToMatch.isEmpty()) {

                    mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                        final Set<String> annotations = new HashSet<>();

                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (methodAnnotationsToMatch.contains(desc)) {
                                annotations.add(desc);
                            }
                            return super.visitAnnotation(desc, visible);
                        }

                        @Override
                        public void visitEnd() {
                            super.visitEnd();

                            addMethodAnnotations(method, annotations);
                            if (addMethodIfMatches(access, method, annotations) && (access & Opcodes.ACC_BRIDGE) != 0) {
                                context.addBridgeMethod(method);
                            }
                        }

                    };
                } else if (addMethodIfMatches(access, method, ImmutableSet.<String> of())
                        && (access & Opcodes.ACC_BRIDGE) != 0) {
                    context.addBridgeMethod(method);
                }

            }
            return mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            Multimap<ClassAndMethodMatcher, String> classMatches;
            Set<Method> daMethods;

            if (matches != null) {
                classMatches = newClassMatches();

                for (ClassAndMethodMatcher matcher : matches.values()) {
                    for (String className : matcher.getClassMatcher().getClassNames()) {
                        classMatches.put(matcher, className);
                    }
                    classMatches.put(matcher, cr.getClassName());
                }
                daMethods = matches.keySet();
                Match match = new Match(classMatches, daMethods, methodAnnotations);
                context.putMatch(OptimizedClassMatcher.this, match);
            }
        }

        private boolean addMethodIfMatches(int access, Method method, Set<String> annotations) {
            boolean match = false;
            Collection<ClassAndMethodMatcher> set = methods.get(method);
            if (set != null) {
                for (ClassAndMethodMatcher matcher : set) {
                    if (isMatch(matcher.getClassMatcher(), loader, cr, classBeingRedefined)) {
                        getOrCreateMatches().put(method, matcher);
                        match = true;
                    }
                }
            }
            for (Entry<MethodMatcher, ClassAndMethodMatcher> entry : methodMatchers) {
                if (entry.getKey().matches(access, method.getName(), method.getDescriptor(), annotations)) {
                    if (isMatch(entry.getValue().getClassMatcher(), loader, cr, classBeingRedefined)) {
                        getOrCreateMatches().put(method, entry.getValue());
                        match = true;
                    }
                }
            }
            return match;
        }
    }

    @Override
    public String toString() {
        return "OptimizedClassMatcher [methodMatchers=" + Arrays.toString(methodMatchers) + ", methods=" + methods
                + ", methodAnnotationsToMatch=" + methodAnnotationsToMatch + ", exactClassNames=" + exactClassNames
                + "]";
    }

}
