/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

public class ScopeTest {
    public boolean changeVar = false;

    /**
     * Weaver will wrap original with try/catch/finally
     * 
     * @return
     */
    public int returnFive() throws Exception {
        return 5;
    }

    public int returnFour() throws Exception {
        return 4;
    }

    public int returnThree() throws Exception {
        return 3;
    }

}
