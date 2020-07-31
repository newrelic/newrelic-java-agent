/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import javax.ws.rs.Path;

public class OrdersSubResource {

    protected IdSubResource idType = new IdSubResource();

    @Path("getStuff")
    public Object getById() {
        return idType;
    }

}