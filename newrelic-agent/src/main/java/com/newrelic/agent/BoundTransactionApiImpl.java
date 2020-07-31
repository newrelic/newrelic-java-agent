/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * 
 * In certain situations it is necessary to tie an instance of this object to a specific instance of an internal
 * Transaction. A one-argument constructor is provided for use in these (unusual, legacy instrumentation) situations. <br>
 * 
 * Use of this class can lead to functional failures in customer code. See JAVA-883. Creation of this class should be
 * restricted to instances that will be used internally, e.g. to support legacy asynchronous servlet instrumentation.
 */
public class BoundTransactionApiImpl extends TransactionApiImpl {
    private final Transaction boundTransaction;

    /**
     * Construct a TransactionApiImpl that will be bound to the argument Transaction.<br>
     * 
     * @param boundTransaction the bound Transaction, which must not be null.
     */
    public BoundTransactionApiImpl(com.newrelic.agent.Transaction boundTransaction) {
        if (boundTransaction == null) {
            throw new IllegalArgumentException("boundTransaction must not be null");
        }
        this.boundTransaction = boundTransaction;
    }

    /**
     * @return the real transaction bound to this object
     */
    @Override
    protected com.newrelic.agent.Transaction getTransactionIfExists() {
        return boundTransaction;
    }

}
