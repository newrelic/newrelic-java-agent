/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import com.google.common.collect.Sets;
import com.newrelic.agent.profile.MethodLineNumberMatcher;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodInfoUtil {

    public static MethodInfo createMethodInfo(Class<?> declaringClass, String methodName, int lineNumber) {

        String methodDesc = MethodLineNumberMatcher.getMethodDescription(declaringClass, methodName, lineNumber);
        return getMethodInfo(declaringClass, methodName, methodDesc);
    }

    protected static MethodInfo getMethodInfo(Class<?> declaringClass, String methodName, String methodDesc) {
        if (methodDesc == null) {
            return handleNoMethodDesc(declaringClass, methodName);
        } else {
            // we should know the exact method
            List<String> args = getArguments(methodDesc);
            if (isConstructor(methodName)) {
                return handleConstructor(declaringClass, methodName, methodDesc, args);
            } else {
                return handleMethod(declaringClass, methodName, methodDesc, args);
            }
        }
    }

    private static MethodInfo handleMethod(Class<?> declaringClass, String methodName, String methodDesc,
            List<String> args) {
        List<Member> members = new ArrayList<>();
        if (getMethod(members, declaringClass, methodName, args)) {
            return new ExactMethodInfo(args, members.get(0));
        } else {
            return new MultipleMethodInfo(new HashSet<>(members));
        }
    }

    private static MethodInfo handleConstructor(Class<?> declaringClass, String methodName, String methodDesc,
            List<String> args) {
        List<Member> members = new ArrayList<>();
        if (getConstructor(members, declaringClass, methodName, args)) {
            return new ExactMethodInfo(args, members.get(0));
        } else {
            return new MultipleMethodInfo(new HashSet<>(members));
        }
    }

    private static MethodInfo handleNoMethodDesc(Class<?> declaringClass, String methodName) {
        if (isConstructor(methodName)) {
            return new MultipleMethodInfo(Sets.<Member> newHashSet(declaringClass.getDeclaredConstructors()));
        } else {
            return new MultipleMethodInfo(getMatchingMethods(declaringClass, methodName));
        }
    }

    protected static List<String> getArguments(Member m) {
        List<String> paramClasses = new ArrayList<>();
        Class<?>[] params;
        if (m instanceof Method) {
            params = ((Method) m).getParameterTypes();
        } else if (m instanceof Constructor) {
            params = ((Constructor<?>) m).getParameterTypes();
        } else {
            params = new Class[0];
        }
        for (Class<?> clazz : params) {
            paramClasses.add(getClassName(clazz));
        }
        return paramClasses;
    }

    protected static List<String> getArguments(String methodDesc) {
        Type[] types = Type.getArgumentTypes(methodDesc);
        List<String> args = new ArrayList<>(types.length);

        for (Type current : types) {
            args.add(current.getClassName());
        }
        return args;
    }

    private static boolean isConstructor(String methodName) {
        return methodName.startsWith("<");
    }

    private static boolean getConstructor(List<Member> addToHere, Class<?> declaringClass, String constName,
            List<String> arguments) {
        for (Constructor<?> constructor : declaringClass.getDeclaredConstructors()) {
            addToHere.add(constructor);
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == arguments.size()) {
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    if (!arguments.get(i).equals(getClassName(params[i]))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    addToHere.clear();
                    addToHere.add(constructor);
                    return true;
                }
            }
        }
        return false;
    }

    static String getClassName(Class<?> clazz) {
        String paramName = null;
        try {
            paramName = clazz.getCanonicalName();
        } catch (Throwable t) {
            // scala classes throw an InternalError
            // ignore and use getName()
        }
        if (null == paramName) {
            return clazz.getName();
        }
        return paramName;
    }

    protected static boolean getMethod(List<Member> addToHere, Class<?> declaringClass, String methodName,
            List<String> arguments) {
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                addToHere.add(method);
                Class<?>[] params = method.getParameterTypes();
                if (params.length == arguments.size()) {
                    boolean matches = true;
                    for (int i = 0; i < params.length; i++) {
                        if (!arguments.get(i).equals(getClassName(params[i]))) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        addToHere.clear();
                        addToHere.add(method);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected static Set<Member> getMatchingMethods(Class<?> declaringClass, String methodName) {
        Set<Member> methods = new HashSet<>();
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                methods.add(method);
            }
        }
        return methods;
    }

}
