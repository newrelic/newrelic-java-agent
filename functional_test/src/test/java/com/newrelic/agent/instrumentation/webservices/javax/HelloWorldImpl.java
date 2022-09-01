/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.javax;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(endpointInterface = "com.newrelic.agent.instrumentation.webservices.javax.HelloWorld")
public class HelloWorldImpl implements HelloWorld, Runnable {

    @Override
    @WebMethod
    public String getHelloWorld(String name) {
        return "Hey what's up " + name;
    }

    @Override
    public void run() {
        // This should not be traced
    }

}
