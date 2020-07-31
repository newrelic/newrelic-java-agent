/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;

@InterfaceMapper(originalInterfaceName = "com/newrelic/agent/instrumentation/Test1",
        className = "com/newrelic/agent/instrumentation/Test1")
public interface InterfaceMapperMissingMethodTest1 {

    @MethodMapper(originalMethodName = "methodDoesNotExist")
    Object _nr_methodDoesNotExist();

}
