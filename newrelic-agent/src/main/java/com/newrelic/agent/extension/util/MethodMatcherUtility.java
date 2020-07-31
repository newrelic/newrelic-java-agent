/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters;
import com.newrelic.agent.extension.beans.MethodParameters;
import com.newrelic.agent.instrumentation.methodmatchers.ExactParamsMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactReturnTypeMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.util.asm.Utils;
import org.objectweb.asm.Type;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MethodMatcherUtility {

    public static MethodMatcher createMethodMatcher(String className, List<Method> methods,
            Map<String, MethodMapper> classesToMethods, String extName) throws XmlException {
        List<MethodMatcher> matchers = new LinkedList<>();
        for (Method method : methods) {
            try {
                matchers.add(createMethodMatcher(className, method, classesToMethods, extName));
            } catch (NoSuchMethodException e) {
                Agent.LOG.warning(e.getMessage());
            }
        }
        if (matchers.size() > 1) {
            return OrMethodMatcher.getMethodMatcher(matchers);
        } else if (matchers.size() == 1) {
            return matchers.get(0);
        } else {
            throw new XmlException("All methods for " + className + " have already been added.");
        }
    }

    public static MethodMatcher createMethodMatcher(String className, Method method,
            Map<String, MethodMapper> classesToMethods, String extName) throws NoSuchMethodException, XmlException {
        if (method == null) {
            throw new XmlException("A method must be specified for a point cut in the extension.");
        }

        if (method.getReturnType() != null) {
            if (Utils.isPrimitiveType(method.getReturnType())) {
                throw new XmlException("The return type '" + method.getReturnType()
                        + "' is not valid.  Primitive types are not allowed.");
            }
            Type returnType = Type.getObjectType(method.getReturnType().replace('.', '/'));

            if (!ExtensionConversionUtility.isReturnTypeOkay(returnType)) {
                throw new XmlException("The return type '" + returnType.getClassName()
                        + "' is not valid.  Primitive types are not allowed.");
            }
            return new ExactReturnTypeMethodMatcher(returnType);
        }

        validateMethod(method, extName);
        String methodName = method.getName();
        if (methodName == null) {
            throw new XmlException("A method name must be specified for a point cut in the extension.");
        }
        methodName = methodName.trim();
        if (methodName.length() == 0) {
            throw new XmlException("A method must be specified for a point cut in the extension.");
        }

        Parameters mParams = method.getParameters();
        if (mParams == null || mParams.getType() == null) {
            if (!isDuplicateMethod(className, methodName, null, classesToMethods)) {
                return new NameMethodMatcher(methodName);
            } else {
                throw new NoSuchMethodException("Method " + methodName
                        + " has already been added to a point cut and will " + "not be added again.");
            }
        } else {
            String descriptor = MethodParameters.getDescriptor(mParams);

            if (descriptor == null) {
                throw new XmlException("Descriptor not being calculated correctly.");
            }

            String mDescriptor = descriptor.trim();
            if (!isDuplicateMethod(className, methodName, mDescriptor, classesToMethods)) {
                return ExactParamsMethodMatcher.createExactParamsMethodMatcher(methodName, descriptor);
            } else {
                throw new NoSuchMethodException("Method " + methodName
                        + " has already been added to a point cut and will " + "not be added again.");
            }
        }
    }

    /**
     * Validates the input method.
     * 
     * @param m The method found in the xml.
     * @param extName The name of the extension.
     * @throws XmlException
     */
    private static void validateMethod(final Method m, final String extName) throws XmlException {
        if (m == null) {
            throw new XmlException(MessageFormat.format(XmlParsingMessages.NO_METHOD, extName));
        } else {
            String mName = m.getName();
            if ((mName == null) || (mName.trim().length() == 0)) {
                throw new XmlException(MessageFormat.format(XmlParsingMessages.NO_METHOD_NAME, extName));
            }
        }
    }

    /**
     * Returns true if the input className, methodName, and descriptor have already been added, meaning the input is a
     * duplicate. Returns true if the class name if null.
     * 
     * @param className The name of the class.
     * @param methodName The name of the method.
     * @param descriptor The descriptor.
     * @param classesToMethods The previous classes and methods.
     * @return True if this combination is a duplicate.
     */
    private static boolean isDuplicateMethod(final String className, final String methodName, final String descriptor,
            final Map<String, MethodMapper> classesToMethods) {
        // convert the class name
        if (className != null) {
            String name = Type.getObjectType(className).getClassName();
            MethodMapper mapper = classesToMethods.get(name);
            if (mapper == null) {
                mapper = new MethodMapper();
                classesToMethods.put(className, mapper);
            }
            return !mapper.addIfNotPresent(methodName, descriptor);
        }
        return true;
    }
}
