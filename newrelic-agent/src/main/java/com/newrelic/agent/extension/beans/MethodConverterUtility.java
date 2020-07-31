/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.beans;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MethodConverterUtility {
    /**
     * The beginning parenthesis for a parameter descriptor.
     */
    private static final String BEGIN_PARENTH_FOR_DESCRIPTOR = "(";
    /**
     * The end parenthesis for a parameter descriptor.
     */
    private static final String END_PARENTH_FOR_DESCRIPTOR = ")";

    /**
     * Designates an array.
     */
    private static final String ARRAY_NOTATION = "[";

    /**
     * Pattern to match for an array.
     */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.+?)((\\[\\])+)\\z");

    /**
     * Used to count the number of array brackets.
     */
    private static final Pattern BRACKETS = Pattern.compile("(\\[\\])");

    /**
     * Designates the type parameter for collections.
     */
    private static final String COLLECTION_TYPE_REGEX = "<.+?>";

    /**
     * Creates this MethodConverterUtility.
     */
    private MethodConverterUtility() {
        super();
    }

    /**
     * Converts an input parameter to the parameter descriptor string.
     *
     * @param inputParam This would be like you see in the method signature. For example the primitive integer should
     * come into this method as "int" meanwhile a string should come in as "java.lang.String"
     * @return The parameter descriptor for the input parameter.
     */
    private static String convertParamToDescriptorFormat(final String inputParam) {
        if (inputParam == null) {
            throw new RuntimeException("The input parameter can not be null.");
        }

        Type paramType;
        // check to see if it is a primitive first
        if (Type.BOOLEAN_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.BOOLEAN_TYPE;
        } else if (Type.BYTE_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.BYTE_TYPE;
        } else if (Type.CHAR_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.CHAR_TYPE;
        } else if (Type.DOUBLE_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.DOUBLE_TYPE;
        } else if (Type.FLOAT_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.FLOAT_TYPE;
        } else if (Type.INT_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.INT_TYPE;
        } else if (Type.LONG_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.LONG_TYPE;
        } else if (Type.SHORT_TYPE.getClassName().equals(inputParam)) {
            paramType = Type.SHORT_TYPE;
        } else {
            // check for array
            Matcher arrayMatcher = ARRAY_PATTERN.matcher(inputParam);
            if (arrayMatcher.matches()) {
                String typeName = arrayMatcher.group(1);
                String brackets = arrayMatcher.group(2);
                return makeArrayType(typeName, brackets);
            } else {
                if (inputParam.contains("[")) {
                    throw new RuntimeException("Brackets should only be in the parameter name if "
                            + "it is an array. Name: " + inputParam);
                }
                // we assume it is an object
                String output = inputParam.replace(".", "/").replaceAll(COLLECTION_TYPE_REGEX, "");
                paramType = Type.getObjectType(output);
            }
        }

        return paramType.getDescriptor();
    }

    /**
     * Makes the array descriptor.
     *
     * @param paramType The name of the parameter.
     * @param brackets The string of brackets from the matcher.
     * @return The array parameter descriptor.
     */
    private static String makeArrayType(final String paramType, final String brackets) {
        // count the number of brackets
        Matcher mms = BRACKETS.matcher(brackets);
        int count = 0;
        while (mms.find()) {
            count++;
        }

        // create the internal representation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ARRAY_NOTATION);
        }
        sb.append(convertParamToDescriptorFormat(paramType));
        return sb.toString();
    }

    /**
     * Takes in a list of input parameters and converts it to the parameter descriptor part of the method descriptor,
     * including the parenthesis.
     *
     * @param inputParameters These would be like you see in the method signature. For example the primitive integer
     * should come into this method as "int" meanwhile a string should come in as "java.lang.String".
     * @return Everything in the method descriptor up to, so not including, the return descriptor.
     */
    protected static String paramNamesToParamDescriptor(final List<String> inputParameters) {
        if (inputParameters == null) {
            return BEGIN_PARENTH_FOR_DESCRIPTOR + END_PARENTH_FOR_DESCRIPTOR;
        } else {
            List<String> descriptors = new ArrayList<>();
            for (String param : inputParameters) {
                descriptors.add(convertParamToDescriptorFormat(param.trim()));
            }
            return convertToParmDescriptor(descriptors);
        }
    }

    /**
     * Converts the list of parameter descriptors to the actual parameterDescriptor portion of the of the method
     * descriptor, including the parenthesis.
     *
     * @param paramDescriptors List of parameters, in the same order as the signature, in parameter descriptor form.
     * @return Everything in the method descriptor up to, so not including, the return descriptor.
     */
    private static String convertToParmDescriptor(final List<String> paramDescriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append(BEGIN_PARENTH_FOR_DESCRIPTOR);
        if (paramDescriptors != null && !paramDescriptors.isEmpty()) {
            for (String param : paramDescriptors) {
                // check the param type
                if (Type.getType(param) == null) {
                    throw new RuntimeException("The generated parameter descriptor is invalid. Name: " + param);
                }
                sb.append(param);
            }
        }
        sb.append(END_PARENTH_FOR_DESCRIPTOR);

        return sb.toString();
    }

}