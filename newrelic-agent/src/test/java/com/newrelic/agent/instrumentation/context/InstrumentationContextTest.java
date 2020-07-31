/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class InstrumentationContextTest {

    @Test
    public void testInterfacesDoesntReturnNull() {
        InstrumentationContext testClass = new InstrumentationContext(null, null, null);
        assertNotNull(testClass.getInterfaces());
    }

}
