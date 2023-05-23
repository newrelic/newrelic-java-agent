/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.Collection;

import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.service.Service;

public interface ClassTransformerService extends Service {

    PointCutClassTransformer getClassTransformer();

    ClassRetransformer getLocalRetransformer();

    ClassRetransformer getRemoteRetransformer();

    void checkShutdown();

    InstrumentationContextManager getContextManager();

    /**
     * Add a matcher that will match class/methods which should be traced.
     * 
     * @return true if the matcher was not previously added
     */
    boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix);

    /**
     * Add a matcher that will match class/methods which should be traced.
     *
     * @return true if the matcher was not previously added
     */
    boolean addTraceMatcher(ClassAndMethodMatcher matcher, TraceDetails traceDetails);

    /**
     * Queues the retransformation of loaded classes that match the given class matchers.
     */
    void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> classMatchers);

    /**
     * Immediately retransforms the loaded classes that match the given class matchers.
     */
    void retransformMatchingClassesImmediately(Class<?>[] loadedClasses, Collection<ClassMatchVisitorFactory> classMatchers);

    /**
     * Returns an Instrumentation instance that inserts added class transformers before the agent's class transformers.
     * This allows extensions to add Trace annotations to class methods and have those annotations be picked up by the
     * agent.
     */
    Instrumentation getExtensionInstrumentation();

    default void retransformForAttach() {}
}
