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

@Path("interface")
public interface InterfaceResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getIt();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String exceptionTest();

}
