/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.proxy;

import com.newrelic.api.agent.Trace;

@SuppressWarnings("UnusedReturnValue")
public interface MyProxyInterface {

    String getSomeString();

    @Trace
    int getSomeInt();

}
