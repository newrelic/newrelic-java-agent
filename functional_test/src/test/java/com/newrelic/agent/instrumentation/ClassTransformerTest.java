/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.pointcuts.MathCSConcurrentPointCut;
import com.newrelic.agent.instrumentation.pointcuts.XmlRpcPointCut;
import com.newrelic.agent.instrumentation.pointcuts.container.JasperCompilerPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.ProcessActionPortletPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.RenderPortletPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf.CXFInvokerPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf.CXFPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf.ClientProxyPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.faces.LifecyclePointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.spring.HandlerInterceptorPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.spring.HandlerMethodInvokerPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.spring.SpringDispatcherPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.spring.SpringExceptionHandlerPointCut;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.spring.SpringPointCut;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;

public class ClassTransformerTest {

    @Test
    public void test() {
        PointCutClassTransformer classTransformer = ServiceFactory.getClassTransformerService().getClassTransformer();

        Collection<PointCut> pcs = new ArrayList<>();
        try {
            Method method = classTransformer.getClass().getDeclaredMethod("findEnabledPointCuts");
            method.setAccessible(true);
            pcs = (Collection<PointCut>) method.invoke(classTransformer);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception occurred: " + e.getMessage());
        }

        List<? extends PointCut> manual = Arrays.asList(
                // Spring
                new SpringPointCut(classTransformer),
                // new SpringWildcardPathPointCut(classTransformer),
                new SpringDispatcherPointCut(classTransformer),
                new HandlerInterceptorPointCut(classTransformer),
                new HandlerMethodInvokerPointCut(classTransformer),
                new SpringExceptionHandlerPointCut(classTransformer),
                // Faces
                new LifecyclePointCut(classTransformer),
                // new PhasePointCut(), // In pointcuts.yml
                // CXF
                new CXFPointCut(classTransformer),
                new CXFInvokerPointCut(classTransformer),
                new ClientProxyPointCut(classTransformer),
                // portlet
                new ProcessActionPortletPointCut(classTransformer),
                new RenderPortletPointCut(classTransformer),
                // Tomcat
                new JasperCompilerPointCut(classTransformer),

                new XmlRpcPointCut(classTransformer),
                // java concurrent
                new MathCSConcurrentPointCut(classTransformer));

        Set<String> manualNamesEnabled = new HashSet<>();
        Set<String> manualNamesDisabled = new HashSet<>();
        for (PointCut pc : manual) {
            if (pc.isEnabled()) {
                manualNamesEnabled.add(pc.getClass().getName());
            } else {
                manualNamesDisabled.add(pc.getClass().getName());
            }
        }

        Set<String> allNames = new HashSet<>();
        for (PointCut pc : pcs) {
            allNames.add(pc.getClass().getName());
        }

        Set<String> missing = new HashSet<>(manualNamesEnabled);
        missing.removeAll(allNames);

        // all of the enabled should be present
        Assert.assertTrue(missing.toString(), allNames.containsAll(manualNamesEnabled));
        // all of the disabled should not be present
        for (String disabled : manualNamesDisabled) {
            Assert.assertFalse(allNames.contains(disabled));
        }
    }

}
