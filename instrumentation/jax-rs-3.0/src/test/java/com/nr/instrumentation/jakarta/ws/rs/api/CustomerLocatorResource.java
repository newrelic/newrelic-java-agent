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

@Path("/customers")
public class CustomerLocatorResource {

    protected IdSubResource idType = new IdSubResource();
    protected FirstLastSubResource firstLastType = new FirstLastSubResource();
    protected OrdersSubResource ordersSubResource = new OrdersSubResource();

    @Path("type-{type}")
    public Object getDatabase(@PathParam("type") String type) {
        if (type.equals("id")) {
            return idType;
        } else if (type.equals("firstLast")) {
            return firstLastType;
        } else {
            return null;
        }
    }

    @Path("orders")
    public Object getOrders() {
        return ordersSubResource;
    }

    @Path("default-get-test")
    @GET
    public String defaultGetTest() {
        return "TEST";
    }

}
