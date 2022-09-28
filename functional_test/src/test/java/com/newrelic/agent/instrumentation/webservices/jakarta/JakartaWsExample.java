/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.jakarta;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class JakartaWsExample {

    @WebMethod
    public String getWebMethod(String name) {
        return "Hey what's up " + name;
    }

    public void run() {
        // This should not be traced
    }

}
