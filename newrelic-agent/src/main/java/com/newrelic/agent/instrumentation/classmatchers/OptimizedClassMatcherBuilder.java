/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactParamsMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import org.objectweb.asm.commons.Method;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link OptimizedClassMatcher} builder. Builders are not thread safe.
 */
public class OptimizedClassMatcherBuilder {
    private OptimizedClassMatcherBuilder() {
    }

    public static OptimizedClassMatcherBuilder newBuilder() {
        return new OptimizedClassMatcherBuilder();
    }

    private static final Supplier<Set<ClassAndMethodMatcher>> CLASS_AND_METHOD_MATCHER_SET_SUPPLIER = () -> Collections.newSetFromMap(new HashMap<>());

    /**
     * A map of method matchers to their ClassAndMethodMatcher. This will contain method matchers which loosely match
     * methods, like the {@link AllMethodsMatcher} or {@link ExactParamsMethodMatcher}.
     */
    private final SetMultimap<MethodMatcher, ClassAndMethodMatcher> methodMatchers = Multimaps.newSetMultimap(
            new HashMap<>(), CLASS_AND_METHOD_MATCHER_SET_SUPPLIER);

    /**
     * A set multimap of exact methods to match to the ClassAndMethodMatchers which match them.
     */
    private final SetMultimap<Method, ClassAndMethodMatcher> methods = Multimaps.newSetMultimap(
            new HashMap<>(), CLASS_AND_METHOD_MATCHER_SET_SUPPLIER);

    /**
     * A set of method annotation descriptors to match. Note that the class matchers of this guys are not used.
     */
    private final Set<String> methodAnnotationMatchers = new HashSet<>();

    private final Set<String> exactClassNames = new HashSet<>();
    private boolean exactClassMatch = true;

    public OptimizedClassMatcherBuilder addClassMethodMatcher(ClassAndMethodMatcher... matchers) {
        for (ClassAndMethodMatcher matcher : matchers) {
            if (exactClassMatch && !matcher.getClassMatcher().isExactClassMatcher()) {
                exactClassMatch = false;
            } else {
                exactClassNames.addAll(matcher.getClassMatcher().getClassNames());
            }
            if (matcher.getMethodMatcher() instanceof AnnotationMethodMatcher) {
                methodAnnotationMatchers.add(((AnnotationMethodMatcher) matcher.getMethodMatcher()).getAnnotationType().getDescriptor());
            }
            Method[] exactMethods = matcher.getMethodMatcher().getExactMethods();
            if (exactMethods == null || exactMethods.length == 0) {
                methodMatchers.put(matcher.getMethodMatcher(), matcher);
            } else {
                for (Method m : exactMethods) {
                    if (OptimizedClassMatcher.METHODS_WE_NEVER_INSTRUMENT.contains(m)) {
                        Agent.LOG.severe("Skipping method matcher for method " + m);
                        Agent.LOG.fine("Skipping matcher for class matcher " + matcher.getClassMatcher());
                    } else {
                        if (OptimizedClassMatcher.DEFAULT_CONSTRUCTOR.equals(m)) {
                            Agent.LOG.severe("Instrumentation is matching a default constructor.  This may result in slow class loading times.");
                            Agent.LOG.debug("No arg constructor matcher: " + matcher.getClassMatcher());
                        }
                        methods.put(m, matcher);
                    }
                }
            }
        }
        return this;
    }

    public ClassMatchVisitorFactory build() {
        if (methodMatchers.isEmpty() && methods.isEmpty() && methodAnnotationMatchers.isEmpty()) {
            Agent.LOG.finest("Creating an empty class/method matcher");
            return OptimizedClassMatcher.EMPTY_MATCHER;
        }
        Set<String> exactClassNames = null;
        if (exactClassMatch) {
            exactClassNames = this.exactClassNames;
        }
        return new OptimizedClassMatcher(methodAnnotationMatchers, methods, methodMatchers, exactClassNames);
    }

}
