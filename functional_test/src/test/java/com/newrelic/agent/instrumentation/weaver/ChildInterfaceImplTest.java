/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

class ChildInterfaceImpl implements ChildInterface {

    @Override
    public ChildInterface foo() {
        return new ChildInterfaceImpl();
    }

}

public class ChildInterfaceImplTest {
    ChildInterfaceImpl obj;

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(ChildInterfaceImpl.class.getName());
    }

    @Before
    public void before() {
        obj = new ChildInterfaceImpl();
    }

    @Test
    public void testNull() {
        // Instrumentation ignores original return type and instead returns null.
        Assert.assertEquals(null, obj.foo());
    }
}
