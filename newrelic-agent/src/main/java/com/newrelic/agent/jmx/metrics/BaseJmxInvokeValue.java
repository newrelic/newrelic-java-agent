/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

/**
 * Some application servers such as glassfish require you to invoke the boot method to get any jmx metrics. This
 * framework is here for this.
 */
public class BaseJmxInvokeValue {

    /** The object name for the jmx metric. */
    private final String objectNameString;

    /** The name of the operation. */
    private final String operationName;

    /** The parameters. */
    private final Object[] params;

    /** the signature. */
    private final String[] signature;

    public BaseJmxInvokeValue(final String pObjectName, final String pOperationName, final Object[] pParams,
            final String[] pSignature) {
        super();
        objectNameString = pObjectName;
        operationName = pOperationName;
        params = pParams;
        signature = pSignature;
    }

    /**
     * Gets the field objectNameString.
     * 
     * @return the objectNameString
     */
    public String getObjectNameString() {
        return objectNameString;
    }

    /**
     * Gets the field operationName.
     * 
     * @return the operationName
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Gets the field params.
     * 
     * @return the params
     */
    public Object[] getParams() {
        return params;
    }

    /**
     * Gets the field signature.
     * 
     * @return the signature
     */
    public String[] getSignature() {
        return signature;
    }

}
