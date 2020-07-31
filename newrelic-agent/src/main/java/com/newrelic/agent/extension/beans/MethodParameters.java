/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.beans;

import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Takes in the list of parameters.
 */
public class MethodParameters extends Parameters {

    /**
     * The parameter descriptor. This is the method descriptor minus the return descriptor.
     */
    private final String paramDescriptor;

    /**
     * True means there was an error converting from Parameters to this Method Parameters.
     */
    private final boolean wasError;

    /**
     * The error message.
     */
    private final String errorMessage;

    /**
     * Creates this MethodParameters.
     *
     * @param pParams The input parameters to the method, in order.
     */
    public MethodParameters(final List<String> pParams) {
        super();
        if (pParams != null) {
            List<Type> types = new ArrayList<>(pParams.size());
            for (String p : pParams) {
                Type t = new Type();
                t.setValue(p);
                types.add(t);
            }
            type = types;
        }

        String desc;
        boolean pError;
        String eMessage;

        // get the param descriptor
        try {
            desc = MethodConverterUtility.paramNamesToParamDescriptor(pParams);
            pError = false;
            eMessage = "";
        } catch (Exception e) {
            pError = true;
            eMessage = e.getMessage();
            desc = null;
        }

        // set the parameters
        paramDescriptor = desc;
        wasError = pError;
        errorMessage = eMessage;
    }

    /**
     * Returns the parameter descriptor.
     *
     * @return The parameter descriptor.
     */
    public String getDescriptor() {
        return paramDescriptor;
    }

    /**
     * Gets the field wasError.
     *
     * @return the wasError
     */
    public boolean isWasError() {
        return wasError;
    }

    /**
     * Gets the field errorMessage.
     *
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public static String getDescriptor(Parameters parameters) {
        return new MethodParameters(parameters == null ? Collections.<String>emptyList()
                : convertToStringList(parameters.getType())).getDescriptor();
    }

    private static List<String> convertToStringList(List<Type> types) {
        List<String> params = new ArrayList<>(types.size());
        for (Type t : types) {
            params.add(t.getValue());
        }
        return params;
    }
}
