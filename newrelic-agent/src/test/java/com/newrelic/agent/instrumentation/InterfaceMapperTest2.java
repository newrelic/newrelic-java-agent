/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;

@InterfaceMapper(originalInterfaceName = "com/newrelic/agent/instrumentation/Test2",
        className = "com/newrelic/agent/instrumentation/Test2")
public interface InterfaceMapperTest2 {

    @MethodMapper(originalMethodName = "test1", originalDescriptor = "()Ljava/lang/String;")
    Object _nr_test1();

}
