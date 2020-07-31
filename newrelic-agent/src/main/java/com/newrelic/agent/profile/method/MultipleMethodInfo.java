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
import java.util.Set;

import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.util.StringMap;

public class MultipleMethodInfo extends MethodInfo {

    private final Set<Member> possibleMethods;

    public MultipleMethodInfo(Set<Member> methods) {
        possibleMethods = methods;
    }
    
    @Override
    public List<Map<String, Object>> getJsonMethodMaps(StringMap stringMap) {
        List<Map<String, Object>> methodList = new ArrayList<>();

        for (Member current : possibleMethods) {
            Map<String, Object> oneMethod = getMethodMap(stringMap, MethodInfoUtil.getArguments(current), ((AnnotatedElement) current).getAnnotation(InstrumentedMethod.class));
            methodList.add(oneMethod);
        }

        return methodList;
    }
}
