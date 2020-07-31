/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Map;

/**
 * A new instance of this class will be created for every usage of {@link AnnotationProxyTemplate}. The purpose of this
 * class is to redirect calls on the annotation proxy to our annotationValuesMap in order to look up the real value at
 * runtime.
 */
public class AnnotationProxyInvocationHandler implements InvocationHandler {

    // The key for this map will be the name of a method on an annotation and the value could be anything from a String
    // to an int, to an array to even yet another AnnotationProxyTemplate.
    private final Map<String, Object> annotationValuesMap;

    public AnnotationProxyInvocationHandler(Map<String, Object> annotationValuesMap) {
        this.annotationValuesMap = annotationValuesMap;
    }

    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Object value = annotationValuesMap.get(methodName);
        if (value instanceof ArrayList) {
            // When we match an ArrayList we want to return back an array because Annotations can only hold Arrays
            // as their collection type, not Lists or any other Java Collection.
            return ((ArrayList) value).toArray();
        }

        if (value == null) {
            // This will attempt to get the default value from an annotation method if it wasn't found in the map above
            value = method.getDefaultValue();
        }

        // If the map didn't contain the method above (such as toString, hashCode, etc) this will return null
        return value;
    }

}
