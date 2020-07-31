/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.profile.method.ExactMethodInfo;
import com.newrelic.agent.profile.method.MethodInfo;
import com.newrelic.agent.profile.method.MethodInfoFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import org.json.simple.JSONStreamAware;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfiledMethodFactory {
    
    /**
     * A map of stack trace elements to ProfiledMethods. This helps us create fewer {@link ProfiledMethod} instances and
     * allows us to use identity hashmaps.
     * 
     * This map is accessed concurrently because it is used for tracers.
     */
    private final ConcurrentMap<?, ProfiledMethod> profiledMethods = new ConcurrentHashMap<>();
    /**
     * Class name token to file name token.
     */
    private final ConcurrentMap<Object, Object> classFileNames = new ConcurrentHashMap<>();
    private final IProfile profile;
    private final AtomicInteger nextMethodId = new AtomicInteger();
    private final ConcurrentMap<ClassMethodSignature, MethodInfo> methodInfos = new ConcurrentHashMap<>();

    public ProfiledMethodFactory(IProfile profile) {
        this.profile = profile;
    }

    public ProfiledMethod getProfiledMethod(StackTraceElement stackTraceElement) {
        ProfiledMethod method = getStackTraceMethods().get(stackTraceElement);
        if (method == null) {
            method = ProfiledMethod.newProfiledMethod(getNextMethodId(), profile, stackTraceElement);
            if (method != null) {
                if (stackTraceElement.getFileName() != null) {
                    classFileNames.put(profile.getStringMap().addString(stackTraceElement.getClassName()), 
                            profile.getStringMap().addString(stackTraceElement.getFileName()));
                }

                ProfiledMethod existing = getStackTraceMethods().putIfAbsent(stackTraceElement, method);
                if (null != existing) {
                    return existing;
                }
            }
        }
        return method;
    }
    
    @SuppressWarnings("unchecked")
    private ConcurrentMap<StackTraceElement, ProfiledMethod> getStackTraceMethods() {
        return (ConcurrentMap<StackTraceElement, ProfiledMethod>) profiledMethods;
    }
    
    @SuppressWarnings("unchecked")
    private ConcurrentMap<ClassMethodSignature, ProfiledMethod> getTracerMethods() {
        return (ConcurrentMap<ClassMethodSignature, ProfiledMethod>) profiledMethods;
    }
    
    public ProfiledMethod getProfiledMethod(Tracer tracer) {
        ClassMethodSignature methodSignature = tracer.getClassMethodSignature();
        ProfiledMethod method = getTracerMethods().get(methodSignature);
        if (method == null) {
            StackTraceElement stackTraceElement = new StackTraceElement(
                    methodSignature.getClassName(), methodSignature.getMethodName(), null, -1);
            method = ProfiledMethod.newProfiledMethod(getNextMethodId(), profile, stackTraceElement);
            if (method != null) {
                ProfiledMethod previous = getTracerMethods().putIfAbsent(methodSignature, method);
                if (null != previous) {
                    method = previous;
                } else {
                    MethodInfo methodInfo = getMethodInfo(methodSignature);
                    method.setMethodInfo(methodInfo);
                }
            }
        }
        return method;
    }
    
    public MethodInfo getMethodInfo(ClassMethodSignature methodSignature) {
        MethodInfo methodInfo = methodInfos.get(methodSignature);
        
        if (null == methodInfo) {
        
            Type[] argumentTypes = Type.getArgumentTypes(methodSignature.getMethodDesc());
    
            List<String> arguments = new ArrayList<>(argumentTypes.length);
            for (Type t : argumentTypes) {
                arguments.add(t.getClassName());
            }
            methodInfo = new ExactMethodInfo(arguments, (InstrumentedMethod)null);
            MethodInfo previous = methodInfos.putIfAbsent(methodSignature, methodInfo);
            if (previous != null) {
                return previous;
            }
        }
        
        return methodInfo;
    }

    private String getNextMethodId() {
        int id = nextMethodId.getAndIncrement();
        return Integer.toHexString(id);
    }

    public void setMethodDetails(MethodInfoFactory methodInfoFactory) {
        for (ProfiledMethod method : profiledMethods.values()) {
            method.setMethodDetails(methodInfoFactory);
        }
    }

    public Map<String, JSONStreamAware> getMethods() {
        Map<String, JSONStreamAware> map = new HashMap<>();
        
        for (ProfiledMethod method : profiledMethods.values()) {
            map.put(method.getMethodId(), method.getMethodJson());
        }
        
        return map;
    }

    public Map<Object,Object> getClasses() {
        return classFileNames;
    }

}
