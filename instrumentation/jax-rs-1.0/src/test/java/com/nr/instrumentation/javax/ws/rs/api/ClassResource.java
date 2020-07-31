/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("class")
public class ClassResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        String data = getData();
        try {
            data = getData();
        } catch (RuntimeException e) {
            return "Oops!";
        }

        return "Got it!";
    }

    @PATCH
    @Consumes("application/json")
    public String patchIt() {
        return "Patched it!";
    }

    @POST
    @Consumes("application/json")
    public String postIt() {
        return "Posted it!";
    }

    @PUT
    @Consumes("application/json")
    public String putIt() {
        return "Put it!";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String exceptionTest() {
        String data;
        try {
            data = getData();
        } catch (RuntimeException e) {
            return "Oops!";
        }

        return data;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String largeNumberOfParametersTest(String one, String two, int three, long four, double five, String six,
            Object seven, Long eight, Integer nine, long ten) {
        String data;
        try {
            data = getData();
        } catch (RuntimeException e) {
            return "Oops!";
        }

        String one1;
        String two2;
        int three3;
        long four4;
        double five5;
        String six6;
        Object seven7;
        Long eight8;
        Integer nine9;
        long ten10;

        String result = "";
        try {
            one1 = "a";
            two2 = "b";
            three3 = 3;
            four4 = 4;
            five5 = 5;
            six6 = "c";
            seven7 = new Object();
            eight8 = 8L;
            nine9 = 9;
            ten10 = 10;

            result = anotherLargeParameterMethod(one1, two2, three3, four4, five5, six6, seven7, eight8, nine9, ten10, "blah",
                    "woot", "cool");
        } catch (Exception e) {

        }

        return result;
    }

    public String anotherLargeParameterMethod(String one, String two, int three, long four, double five, String six,
            Object seven, Long eight, Integer nine, long ten, String blah, String woot, String cool) {
        return getData();
    }


    public String getData() throws RuntimeException {
        return "Got it!";
    }

}
