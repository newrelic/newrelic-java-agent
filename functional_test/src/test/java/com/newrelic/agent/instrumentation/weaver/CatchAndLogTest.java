/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CatchAndLogTest {

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(CatchAndLogContext.class.getName());
    }

    @Test
    public void testCatchAndLogListeners() {
        CatchAndLogContext context = new CatchAndLogContext();

        // weaved class will set a catchAndLogListener annotated with @CatchAndLog on listen()
        assertNotNull(context.catchAndLogListener);

        // catchAndLogListener should log, not throw
        context.catchAndLogListener.listen();

        // throws listener should throw
        try {
            context.throwsListener.listen();
            Assert.fail("Method should have thrown");
        } catch (Throwable t) {
        }
    }
}
