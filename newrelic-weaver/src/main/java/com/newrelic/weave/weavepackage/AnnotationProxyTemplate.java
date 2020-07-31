/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is used as a template and copied whenever {@link com.newrelic.api.agent.weaver.Weaver#getMethodAnnotation(Class)}
 * or {@link com.newrelic.api.agent.weaver.Weaver#getClassAnnotation(Class)} is called in order to define a new class
 * that can be used to statically return a proxy representing the given annotation on the class or method.
 */
public class AnnotationProxyTemplate {

    private static AtomicReference<Annotation> ANNOTATION_PROXY = new AtomicReference<>();

    public static <T extends Annotation> T getOrCreateAnnotationHolder(Class clazz, Class<T> annotationClass,
            final Object... annotationValues) {
        if (ANNOTATION_PROXY.get() == null && clazz != null) {
            // We don't have a cached proxy yet, let's create it
            Object annotationProxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { annotationClass },
                    new AnnotationProxyInvocationHandler(annotationValuesToMap(annotationValues)));

            // If we raced here only one of the threads will win
            ANNOTATION_PROXY.compareAndSet(null, (T) annotationProxy);
        }
        return (T) ANNOTATION_PROXY.get();
    }

    /*
     * Convert the array of key,value annotation data into map form for easier lookup in the proxy
     */
    private static Map<String, Object> annotationValuesToMap(Object... annotationValues) {
        Map<String, Object> annotationMap = new HashMap<>();
        if (annotationValues != null) {
            // loop through key-value pairs of the annotation. The first element is the key and the following element
            // is always the value, so we can loop through using i += 2 here instead of just i++.
            for (int i = 0; i < annotationValues.length; i += 2) {
                annotationMap.put((String) annotationValues[i], annotationValues[i + 1]);
            }
        }
        return annotationMap;
    }

}
