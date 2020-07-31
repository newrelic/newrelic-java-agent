/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ExceptionHandlerSignature implements JSONStreamAware {

    private final String className;
    private final String methodName;
    private final String methodDescription;

    public ExceptionHandlerSignature(String className, String methodName, String methodDescription)
            throws InvalidMethodDescriptor {
        this.className = className;
        this.methodName = methodName;
        this.methodDescription = methodDescription;
        new ExactMethodMatcher(methodName, methodDescription).validate();
    }

    public ExceptionHandlerSignature(ClassMethodSignature sig) throws InvalidMethodDescriptor {
        this.className = sig.getClassName();
        this.methodName = sig.getMethodName();
        this.methodDescription = sig.getMethodDesc();
        new ExactMethodMatcher(methodName, methodDescription).validate();
    }

    private static Collection<String> getExceptionClassNames() {
        // common exception classes
        List<Class<? extends Throwable>> classes = Arrays.asList(Throwable.class, Error.class, Exception.class);
        Collection<String> classNames = new ArrayList<>();
        for (Class clazz : classes) {
            classNames.add(clazz.getName());
        }
        classNames.add("javax.servlet.ServletException");

        return classNames;
    }

    public int getExceptionArgumentIndex() {
        Type[] types = Type.getArgumentTypes(methodDescription);
        Collection<String> exceptionClassNames = getExceptionClassNames();

        for (int i = 0; i < types.length; i++) {
            if (exceptionClassNames.contains(types[i].getClassName())) {
                return i;
            }
        }

        return -1;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescription() {
        return methodDescription;
    }

    public ClassMatcher getClassMatcher() {
        return new ExactClassMatcher(className);
    }

    public MethodMatcher getMethodMatcher() {
        return new ExactMethodMatcher(methodName, methodDescription);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(className, methodName, methodDescription), out);
    }

    @Override
    public String toString() {
        return className.replace('/', '.') + '.' + methodName + methodDescription;
    }

}
