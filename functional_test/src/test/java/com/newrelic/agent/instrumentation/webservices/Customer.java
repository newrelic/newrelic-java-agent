/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/customer")
public interface Customer {

    @Path("/create")
    @PUT
    public Object create(@QueryParam("name") String name, @QueryParam("pwd") String pwd, @QueryParam("mail") String mail);
}
