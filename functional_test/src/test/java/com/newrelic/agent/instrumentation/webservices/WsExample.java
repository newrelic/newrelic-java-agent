/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class WsExample {

    @WebMethod
    public String getWebMethod(String name) {
        return "Hey what's up " + name;
    }

    public void run() {
        // This should not be traced
    }

}
