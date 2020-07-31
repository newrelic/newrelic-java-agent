/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import javax.management.MalformedObjectNameException;

/**
 * Used for methods which need to be invoked on the server.
 * 
 * @since Mar 13, 2013
 */
public class JmxInvoke extends JmxObject {

    /** The name of the operation. */
    private final String operationName;

    /** The parameters. */
    private final Object[] params;

    /** the signature. */
    private final String[] signature;

    private int errorCount = 0;

    /**
     * 
     * Creates this JmxInvoke.
     * 
     * @param pObjectName The name of the object.
     * @param safeName The safe name.
     * @param pOperationName The operation name.
     * @param pParams The parameters for the invoke.
     * @param pSignature The signature.
     * @throws MalformedObjectNameException Thrown if a problem with the object name.
     */
    public JmxInvoke(final String pObjectName, String safeName, final String pOperationName, final Object[] pParams,
            final String[] pSignature) throws MalformedObjectNameException {
        super(pObjectName, safeName);
        operationName = pOperationName;
        params = pParams;
        signature = pSignature;
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

    /**
     * Gets the field errorCount.
     * 
     * @return the errorCount
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Sets the field errorCount.
     */
    public void incrementErrorCount() {
        errorCount++;
    }

    /**
     * String representation of this JmxObject.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object_name: ").append(getObjectNameString());
        sb.append(" operation_name: ").append(operationName);
        sb.append(" error_count: ").append(errorCount);
        return sb.toString();
    }

}
