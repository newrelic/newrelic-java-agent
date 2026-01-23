/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class NewRelicApiTest {

    /**
     * This method will fail if a method is added to the NewRelic class.
     * If your change broke this test, could you add the method to one of the interfaces that can be returned from the Agent?
     */
    @Test
    public void checkMethodCount() {
        Method[] methods = NewRelic.class.getDeclaredMethods();
        Assert.assertEquals(32, methods.length);
    }
}
