/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.Trace;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import org.apache.struts2.ActionProxy;
import org.apache.struts2.result.Result;

import java.util.ArrayList;

public class StrutsSampleApp {
    @Trace(dispatcher = true)
    public static void executeOnActionProxy(ActionProxy proxy) throws Exception {
        proxy.execute();
    }

    @Trace(dispatcher = true)
    public static void executeOnResult(Result result) throws Exception {
        MockActionInvocation actionInvocation = new MockActionInvocation();
        actionInvocation.setAction(new ArrayList<String>());
        result.execute(actionInvocation);
    }
}
