/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/user")
public interface UserService {

    @GET
    @Path("/{id}/features")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserFeatures();

}
