/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.util.StringMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MethodInfo {

    public abstract List<Map<String, Object>> getJsonMethodMaps(StringMap stringMap);
    
    @Deprecated
    public final List<Map<String, Object>> getJsonMethodMaps() {
        return getJsonMethodMaps(StringMap.NO_OP_STRING_MAP);
    }

    private static void addOneMethodInstrumentedInfo(Map<String, Object> toAdd, InstrumentedMethod instrumentedMethod) {
        if (instrumentedMethod != null) {
            Map<String, Object> inst = new HashMap<>();
            inst.put("dispatcher", instrumentedMethod.dispatcher());
            addInstrumentationInfo(inst, instrumentedMethod);

            toAdd.put("traced_instrumentation", inst);
        }
    }

    private static void addInstrumentationInfo(Map<String, Object> inst, InstrumentedMethod instrumentedMethod) {
        addInstrumentationInfo(inst, instrumentedMethod, StringMap.NO_OP_STRING_MAP);
    }

    private static void addInstrumentationInfo(Map<String, Object> inst, InstrumentedMethod instrumentedMethod, StringMap stringMap) {
        InstrumentationType[] inputTypes = instrumentedMethod.instrumentationTypes();
        String[] inputNames = instrumentedMethod.instrumentationNames();

        if (inputTypes != null && inputNames != null && inputTypes.length > 0) {
            if (inputTypes.length == inputNames.length) {
                Map<String, List<Object>> instrumentedTypes = new HashMap<>();

                for (int i = 0; i < inputTypes.length; i++) {
                    if (isTimedInstrumentation(inputTypes[i])) {
                        List<Object> names = instrumentedTypes.get(inputTypes[i].toString());
                        if (names == null) {
                            names = new ArrayList<>();
                            names.add(stringMap.addString(inputNames[i]));
                            instrumentedTypes.put(inputTypes[i].toString(), names);
                        } else {
                            names.add(stringMap.addString(inputNames[i]));
                        }
                    }
                }

                if (instrumentedTypes.size() > 0) {
                    inst.put("types", instrumentedTypes);
                }
            }
        }
    }
    
    static Map<String, Object> getMethodMap(StringMap stringMap, List<String> arguments, InstrumentedMethod annotation) {
        Map<String, Object> map = new HashMap<>();
        addOneMethodArgs(stringMap, map, arguments);
        addOneMethodInstrumentedInfo(map, annotation);
        
        return map;
    }

    private static boolean isTimedInstrumentation(InstrumentationType type) {
        return (type != InstrumentationType.WeaveInstrumentation);
    }
    
    private static void addOneMethodArgs(StringMap stringMap, Map<String, Object> toAdd, List<String> arguments) {
        List<Object> tokens = new ArrayList<>(arguments.size());
        for (String arg : arguments) {
            tokens.add(stringMap.addString(arg));
        }
        toAdd.put("args", tokens);
    }

}
