/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import jakarta.ws.rs.Path;

@Path("v1/people")
public interface PeopleInterfaceResource extends PeopleAPI {

    @Path("/getPeople")
    String getPeople();

}
