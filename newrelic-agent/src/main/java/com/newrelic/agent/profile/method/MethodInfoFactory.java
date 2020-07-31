/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MethodInfoFactory {
    
    private final ConcurrentMap<MethodKey, MethodInfo> methods = new ConcurrentHashMap<>();
    private final ImmutableMap<String, Class<?>> classMap;
    
    public MethodInfoFactory() {
        this(ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses());
    }

    public MethodInfoFactory(Class[] allLoadedClasses) {
        Map<String, Class<?>> classMap = Maps.newHashMapWithExpectedSize(allLoadedClasses.length);
        for (Class<?> clazz : allLoadedClasses) {
            classMap.put(clazz.getName(), clazz);
        }
        this.classMap = ImmutableMap.copyOf(classMap);
    }

    public MethodInfo getMethodInfo(String className, String methodName, int lineNumber) {
        
        MethodKey key = new MethodKey(className, methodName, lineNumber);
        MethodInfo methodInfo = methods.get(key);
        
        if (null == methodInfo) {
            
            Class<?> declaringClass = classMap.get(className);
            if (declaringClass == null) {
                return null;
            }
            
            methodInfo = MethodInfoUtil.createMethodInfo(declaringClass, methodName, lineNumber);
            MethodInfo previous = methods.putIfAbsent(key, methodInfo);
            if (previous != null) {
                return previous;
            }
        }
        
        return methodInfo;
    }
    
    private static final class MethodKey {

        final String className;
        final String methodName;
        final int lineNumber;
        public MethodKey(String className, String methodName, int lineNumber) {
            super();
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + lineNumber;
            result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MethodKey other = (MethodKey) obj;
            if (className == null) {
                if (other.className != null)
                    return false;
            } else if (!className.equals(other.className))
                return false;
            if (lineNumber != other.lineNumber)
                return false;
            if (methodName == null) {
                if (other.methodName != null)
                    return false;
            } else if (!methodName.equals(other.methodName))
                return false;
            return true;
        }
        
    }
}
