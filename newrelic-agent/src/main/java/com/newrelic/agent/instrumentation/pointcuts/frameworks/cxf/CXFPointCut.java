/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class CXFPointCut extends TracerFactoryPointCut {
    static final String CXF_ENDPOINT_ADDRESS_PARAMETER_NAME = "cfx_end_point";

    public CXFPointCut(PointCutClassTransformer classTransformer) {
        super(
                CXFPointCut.class,
                new ExactClassMatcher("org/apache/cxf/transport/servlet/ServletDestination"),
                createExactMethodMatcher(
                        "invoke",
                        "(Ljavax/servlet/ServletConfig;Ljavax/servlet/ServletContext;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, final Object servletDest, Object[] args) {
        try {
            Object endpointInfo = servletDest.getClass().getMethod("getEndpointInfo").invoke(servletDest);
            Object address = endpointInfo.getClass().getMethod("getAddress").invoke(endpointInfo);

            transaction.getInternalParameters().put(CXF_ENDPOINT_ADDRESS_PARAMETER_NAME, address);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, "Error parsing CXF transaction", t);
        }
        return new DefaultTracer(transaction, sig, servletDest, new ClassMethodMetricNameFormat(sig, servletDest));
    }
}
