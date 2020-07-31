/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("v1/people")
public class PeopleResource implements PeopleInterfaceResource {

    @GET
    @Path("/getPeople")
    @Override
    public String getPeople() {
        return "Returning from getPeople";
    }

}
