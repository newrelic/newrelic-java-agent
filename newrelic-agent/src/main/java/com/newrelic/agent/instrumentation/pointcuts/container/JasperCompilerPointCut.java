/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.container;

import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.jasper.GeneratorVisitTracerFactory;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class JasperCompilerPointCut extends TracerFactoryPointCut {

    public static String CURRENT_JSP_FILE_KEY = "CurrentJspFileKey";

    private static final ClassMatcher CLASS_MATCHER = new ExactClassMatcher("org/apache/jasper/compiler/Compiler");
    // This method is in JSP 2.0 but not JSP 2.1
    private static final MethodMatcher COMPILE_METHOD_1_MATCHER = new ExactMethodMatcher("compile", "(ZZ)V");
    // This method is in JSP 2.0 and JSP 2.1
    private static final MethodMatcher COMPILE_METHOD_2_MATCHER = new ExactMethodMatcher("compile", "(Z)V");
    private static final MethodMatcher METHOD_MATCHER = OrMethodMatcher.getMethodMatcher(COMPILE_METHOD_1_MATCHER,
            COMPILE_METHOD_2_MATCHER);

    public JasperCompilerPointCut(PointCutClassTransformer classTransformer) {
        super(JasperCompilerPointCut.class, CLASS_MATCHER, METHOD_MATCHER);
    }

    @Override
    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object compiler, Object[] args) {
        Tracer parent = tx.getTransactionActivity().getLastTracer();
        if (parent != null && parent instanceof JasperCompilerTracer) {
            // this is JSP 2.0 and both instrumented "compile" methods are on the stack
            return null;
        }
        try {
            Object context = compiler.getClass().getMethod("getCompilationContext").invoke(compiler);
            if (context != null) {
                String page = (String) context.getClass().getMethod("getJspFile").invoke(context);
                if (page != null) {
                    String msg = MessageFormat.format("Compiling JSP: {0}", page);
                    Agent.LOG.fine(msg);
                    /*
                     * Track the current JSP file being compiled.
                     * 
                     * @see com.newrelic.agent.tracers.jasper.GeneratorVisitTracerFactory
                     */
                    GeneratorVisitTracerFactory.noticeJspCompile(tx, page);
                    JasperCompilerTracer tracer = new JasperCompilerTracer(tx, sig, compiler,
                            new SimpleMetricNameFormat("View" + page.replace('.', '_') + "/Compile"));
                    return tracer;
                }
            }
        } catch (Throwable t) {
            Agent.LOG.severe("Unable to generate a Jasper compilation metric: " + t.getMessage());
        }
        return null;
    }

    private final class JasperCompilerTracer extends DefaultTracer {

        public JasperCompilerTracer(Transaction tx, ClassMethodSignature sig, Object object,
                MetricNameFormat metricNameFormatter) {
            // no TT segment. non-metric producer
            super(tx, sig, object, metricNameFormatter, 0);
        }
    }

}
