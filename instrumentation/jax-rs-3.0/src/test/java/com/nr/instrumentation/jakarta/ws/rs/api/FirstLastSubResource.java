/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

public class FirstLastSubResource {

    @GET
    @Produces("application/json")
    public String getAllNames() {
        return "{ \"names\": [ \"FirstName, LastName\", \"SecondName, LastName\", \"ThirdName, LastName\" ] }";
    }

    @GET
    @Path("{first}-{last}")
    @Produces("application/json")
    public String getByFirstLast(@PathParam("first") String firstName, @PathParam("last") String lastName) {
        return "FirstName, LastName";
    }

}