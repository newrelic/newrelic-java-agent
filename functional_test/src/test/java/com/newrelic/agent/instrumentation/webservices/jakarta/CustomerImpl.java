/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.jakarta;

import jakarta.ws.rs.QueryParam;

public class CustomerImpl implements Customer {

    public Object create(@QueryParam("name") String name, @QueryParam("pwd") String pwd, @QueryParam("mail") String mail) {
        return name;
    }

}
