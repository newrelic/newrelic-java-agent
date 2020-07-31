/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.proxy;

public class MyProxyImpl implements MyProxyInterface {

    @Override
    public String getSomeString() {
        return "Hello";
    }

    @Override
    public int getSomeInt() {
        return 0;
    }

}
