/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public class IdSubResource {

    @GET
    @Produces("application/json")
    public String getAll() {
        return "{ \"items\": [ \"one\", \"two\", \"three\" ] }";
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public String getById(@PathParam("id") int id) {
        return "one";
    }

}