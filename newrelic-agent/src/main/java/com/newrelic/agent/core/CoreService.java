/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.core;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.service.Service;

import java.lang.instrument.Instrumentation;

public interface CoreService extends Service {

    InstrumentationProxy getInstrumentation();

    // Otel will use methods in instrumentation that were added in Java 9. There is no way for us to implement
    // the Instrumentation interface from Java 9, since it also depends on classes introduced in Java 9.
    // They get away with it by having the classes that use these new methods being compiled in a newer version of Java
    // and wrap the place they use that class in a try catch block that catches UnsupportedClassVersionError.
    default Instrumentation getRealInstrumentation() {
        return getInstrumentation();
    }

    void shutdownAsync();
}
