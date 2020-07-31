/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("interface")
public interface InterfaceResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getIt();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String exceptionTest();

}
