/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.util.Strings;

@PointCut
public class CXFInvokerPointCut extends TracerFactoryPointCut {

    private static final String CXF = "CXF";

    public CXFInvokerPointCut(PointCutClassTransformer classTransformer) {
        super(
                CXFInvokerPointCut.class,
                new ExactClassMatcher("org/apache/cxf/service/invoker/AbstractInvoker"),
                createExactMethodMatcher("performInvocation",
                        "(Lorg/apache/cxf/message/Exchange;Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object invoker, Object[] args) {
        Object service = args[1];
        Method method = (Method) args[2];
        String address = (String) transaction.getInternalParameters().remove(
                CXFPointCut.CXF_ENDPOINT_ADDRESS_PARAMETER_NAME);
        if (address != null) {
            StringBuilder path = new StringBuilder(address);
            if (!address.endsWith("/")) {
                path.append('/');
            }
            path.append(method.getName());
            setTransactionName(transaction, getCXFRequestUri(address, method));
        } else {
            Agent.LOG.log(Level.FINEST, "The CXF endpoint address is null.");

            String txnName = buildCXFTransactionName(service.getClass().getName(), method.getName());
            setTransactionName(transaction, txnName);
        }

        return new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat(Strings.join('/',
                MetricNames.JAVA, service.getClass().getName(), method.getName())));
    }

    static String buildCXFTransactionName(String serviceClassName, String methodName) {
        // Prevent MGI caused by dynamically generated proxies with numbers.
        // com.sun.proxy.$Proxy243/legacyGlobalStopFilter -> com.sun.proxy.$Proxy/legacyGlobalStopFilter
        return serviceClassName.replaceFirst("\\$Proxy\\d+", "\\$Proxy") + "/" + methodName;
    }

    static String getCXFRequestUri(String address, Method method) {
        try {
            address = new URI(address).getPath();
        } catch (URISyntaxException e) {
        }
        StringBuilder path = new StringBuilder();
        if (!address.startsWith("/")) {
            path.append('/');
        }
        path.append(address);
        if (!address.endsWith("/")) {
            path.append('/');
        }
        path.append(method.getName());
        return path.toString();
    }

    private void setTransactionName(Transaction transaction, String path) {
        if (!transaction.isTransactionNamingEnabled()) {
            return;
        }
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (Agent.LOG.isLoggable(Level.FINER)) {
            if (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using CXF", path);
                Agent.LOG.finer(msg);
            }
        }
        policy.setTransactionName(transaction, path, CXF, TransactionNamePriority.FRAMEWORK);
    }

}
