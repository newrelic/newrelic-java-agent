/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.lang.reflect.Method;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;

@InterfaceMapper(className = "org/springframework/web/method/HandlerMethod",
        originalInterfaceName = "org/springframework/web/method/HandlerMethod")
public interface HandlerMethod {

    @MethodMapper(originalMethodName = "getBridgedMethod", originalDescriptor = "()Ljava/lang/reflect/Method;",
            invokeInterface = false)
    Method _nr_getBridgedMethod();

    @MethodMapper(originalMethodName = "getBean", originalDescriptor = "()Ljava/lang/Object;", invokeInterface = false)
    Object _nr_getBean();

}
