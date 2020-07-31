/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Helps the {@link BytecodeGenProxyBuilder} load argument types for which it has no built in support.
 * 
 * @see BytecodeGenProxyBuilder#addLoader(org.objectweb.asm.Type, VariableLoader)
 */
public interface VariableLoader {

    void load(Object value, GeneratorAdapter methodVisitor);

}
