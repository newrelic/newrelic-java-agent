/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/staticEndpoints")
public class StaticEndpoint {


    @Path("staticMethod")
    @GET
    public static void staticMethod(){
    }

    @Path("/inner")
    static class InnerClass{


        @Path("staticMethod")
        @GET
        public static void innerStatic(){
        }
    }

    abstract class AbstractClass{


        @Path("staticMethod")
        @GET
        public void abstractMethod(){
        }
    }

    class AbstractExtender extends AbstractClass{


        @Path("staticMethod")
        @GET
        public void extendedMethod(){
        }
    }
}
