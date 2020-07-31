/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import org.objectweb.asm.Type;

import com.newrelic.agent.bridge.Transaction;

/**
 * A helper class for invoking {@link BytecodeGenProxyBuilder} proxy interfaces with arguments representing special values such as
 * 'this' or a local variable.
 * 
 * The numeric load methods {@link #load(Number, Runnable)} and {@link #loadLocal(int, Type, Number)} take an
 * identifying value which should not collide with any other numeric values used when invoking a proxy method.
 * 
 * @see BytecodeGenProxyBuilder#getVariables()
 */
public interface Variables {

    /**
     * Returns an object which represents 'this', which is null for a static method.
     * 
     * @param access
     */
    Object loadThis(int access);

    /**
     * Returns a Transaction which loads the current transaction onto the stack.
     * 
     */
    Transaction loadCurrentTransaction();

    <N extends Number> N loadLocal(int local, Type type, N value);

    /**
     * Defers the loading of a numeric argument to runnable. The value that's passed in should not equal the numeric
     * value of any other argument or you'll run into trouble.
     * 
     * @param value
     * @param runnable
     */
    <N extends Number> N load(N value, Runnable runnable);

    <O> O load(Class<O> clazz, Runnable runnable);

    /**
     * Returns an object that will load the local variable represented by the given id. Interfaces, Strings and arrays
     * are supported.
     * 
     * @param localId
     * @param clazz
     */
    <O> O loadLocal(int localId, Class<O> clazz);

}
