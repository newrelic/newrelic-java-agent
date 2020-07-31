/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.util.StringMap;

public class ExactMethodInfo extends MethodInfo {

    private final List<String> arguments;
    private final InstrumentedMethod annotation;

    public ExactMethodInfo(List<String> pArguments, Member method) {
        this(pArguments, ((AnnotatedElement) method).getAnnotation(InstrumentedMethod.class));
    }
    
    public ExactMethodInfo(List<String> arguments, InstrumentedMethod annotation) {
        super();
        this.arguments = arguments;
        this.annotation = annotation;
    }

    @Override
    public List<Map<String, Object>> getJsonMethodMaps(StringMap stringMap) {
        List<Map<String, Object>> methodList = new ArrayList<>();

        // only one method
        Map<String, Object> oneMethod = getMethodMap(stringMap, arguments, annotation);

        methodList.add(oneMethod);
        return methodList;
    }

}
