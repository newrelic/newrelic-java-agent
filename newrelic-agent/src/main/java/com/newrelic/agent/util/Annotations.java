/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.JarResource;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.jmx.metrics.JmxInit;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * **IMPORTANT**: If you add a new annotation lookup, you'll need to change the buildManifest task to
 * include the new annotation type.
 */
@SuppressWarnings("deprecation") // the pointcut annotations are deprecated, which is not a helpful thing here.
public class Annotations {

    private static Map<Object, Collection<Class<?>>> annotatedClasses;

    public Collection<Class<?>> getInterfaceMapperClasses() {
        return getAnnotationClassesFromManifest(InterfaceMapper.class);
    }

    public Collection<Class<?>> getInterfaceMixinClasses() {
        return getAnnotationClassesFromManifest(InterfaceMixin.class);
    }

    public Collection<Class<?>> getPointCutAnnotatedClasses() {
        return getAnnotationClassesFromManifest(PointCut.class);
    }

    public Collection<Class<?>> getJmxInitClasses() {
        return getAnnotationClassesFromManifest(JmxInit.class);
    }

    /**
     * Returns a collection of classes that bear the mark of the given annotation class.
     */
    private static Collection<Class<?>> getAnnotationClassesFromManifest(Class<? extends Annotation> annotationClass) {
        synchronized (Annotations.class) {
            if (annotatedClasses == null) {
                annotatedClasses = buildAnnotatedClasses();
            }
        }

        return annotatedClasses.containsKey(annotationClass.getName())
            ? annotatedClasses.get(annotationClass.getName())
            : Collections.<Class<?>>emptySet();
    }

    private static Map<Object, Collection<Class<?>>> buildAnnotatedClasses() {
        Map<Object, Collection<Class<?>>> classes = new HashMap<>();
        Properties props = new Properties();

        try (JarResource agentJarFile = AgentJarHelper.getAgentJarResource();
             InputStream is = agentJarFile.getInputStream("PointcutClasses.properties")) {

            if (is == null) {
                return classes;
            }
            props.load(is);
            for (Object annotationClassName : props.keySet()) {
                Object value = props.get(annotationClassName);

                // For the reverse of this logic, see buildSrc - com.nr.builder.PropertySerializer
                if (value instanceof String) {
                    List<Class<?>> annotatedClassList = new LinkedList<>();
                    String[] annotatedClassNames = ((String) value).split(",");
                    for(String annotatedClassName : annotatedClassNames) {
                        annotatedClassList.add(AgentBridge.getAgent().getClass().getClassLoader().loadClass(annotatedClassName));
                    }
                    classes.put(annotationClassName, annotatedClassList);
                }
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, e, "Unable to load pointcut classes");
            return classes;
        }
        return classes;
    }
}
