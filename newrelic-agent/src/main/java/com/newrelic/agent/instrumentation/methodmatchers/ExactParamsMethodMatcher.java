/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

/**
 * This matcher must match the method name and parameters exactly. It does not look at the return type of the method.
 * 
 * @since Sep 20, 2012
 */
public class ExactParamsMethodMatcher implements MethodMatcher {

    /**
     * The name of the method.
     */
    private final String name;

    /**
     * This should be the parameter part of the internal descriptor with the parenthesis. For example, if the whole
     * descriptor is "([Ljava/lang/String;)V", then this should contain "([Ljava/lang/String;)".
     */
    private final String parameterDescriptor;

    /**
     * 
     * Creates this ExactParamsMethodMatcher.
     * 
     * @param pName The name of the method.
     * @param paramDescriptorWithParenthesis The parameter descriptor for the method with parenthesis, meaning the
     *        method descriptor minus the return descriptor. For example, if a method had on input parameter that was a
     *        string, then the input would be "(Ljava/lang/String;)";
     */
    private ExactParamsMethodMatcher(final String pName, final String paramDescriptorWithParenthesis) {
        name = pName;
        parameterDescriptor = paramDescriptorWithParenthesis;

    }

    /**
     * Using a factory method to create this ExactParamsMethodMatcher so that the conversion and verification can be
     * completed.
     * 
     * @param methodName The name of the method.
     * @param inputDescriptor The method descriptor minus the return type.
     * @return The method matcher.
     * @throws RuntimeException Thrown if the input name or parameters are invalid.
     */
    public static ExactParamsMethodMatcher createExactParamsMethodMatcher(final String methodName,
            final String inputDescriptor) throws RuntimeException {
        // check the method name
        if (methodName == null) {
            throw new RuntimeException("Method name can not be null or empty.");
        }
        String methodNameTrimmed = methodName.trim();
        if (methodNameTrimmed.length() == 0) {
            throw new RuntimeException("Method name can not be null or empty.");
        }

        // check the input descriptor
        if (inputDescriptor == null) {
            throw new RuntimeException("Parameter descriptor can not be null or empty.");

        }
        String inputDescriptorTrimmed = inputDescriptor.trim();
        if (inputDescriptorTrimmed.length() == 0) {
            throw new RuntimeException("Parameter descriptor can not be null or empty.");
        }
        return new ExactParamsMethodMatcher(methodNameTrimmed, inputDescriptorTrimmed);
    }

    /**
     * Returns true is this matcher matches in the input parameters.
     * @param pName The name of the method.
     * @param pDesc The descriptor of the method.
     * @return True if this match matches the name and description.
     */
    @Override
    public boolean matches(int access, String pName, String pDesc, Set<String> annotations) {
        return (name.equals(pName) && (pDesc != null) && (pDesc.startsWith(parameterDescriptor)));
    }

    /**
     * Returns the hash code for this object.
     * 
     * @return Hashcode for this ExactParamsMethodMatcher.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parameterDescriptor == null) ? 0 : parameterDescriptor.hashCode());
        return result;
    }

    /**
     * Returns true if the input object equals this object.
     * 
     * @param obj The input object to compare.
     * @return True if the input object equals this object.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExactParamsMethodMatcher other = (ExactParamsMethodMatcher) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (parameterDescriptor == null) {
            if (other.parameterDescriptor != null) {
                return false;
            }
        } else if (!parameterDescriptor.equals(other.parameterDescriptor)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the string representation of this class.
     */
    @Override
    public String toString() {
        return "ExactParamsMethodMatcher(" + name + ", " + parameterDescriptor + ")";
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }

}
