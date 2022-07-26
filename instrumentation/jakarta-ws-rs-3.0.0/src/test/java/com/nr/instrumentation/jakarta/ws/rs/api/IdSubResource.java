/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

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