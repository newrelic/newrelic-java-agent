/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("/user")
public class RestExample {

    @Path("/create")
    @PUT
    public Object create(@QueryParam("name") String name, @QueryParam("pwd") String pwd, @QueryParam("mail") String mail) {
        return "create";
    }

    @GET
    public List<Object> dude() {
        return new ArrayList<>();
    }

    // intentionally leave slash off of list path to verify that we generate the right metric
    @Path("list")
    @GET
    public List<Object> list(@QueryParam("first") @DefaultValue("0") int first,
            @QueryParam("max") @DefaultValue("20") int max) {
        return new ArrayList<>();
    }

    @Path("/show/{id}")
    @GET
    public Object find(@PathParam("id") long id) {
        return "find";
    }

    @Path("/delete/{id}")
    @DELETE
    public void delete(@PathParam("id") long id) {

    }

    @Path("/update/{id}")
    @POST
    public Object update(@PathParam("id") long id, @QueryParam("name") String name, @QueryParam("pwd") String pwd,
            @QueryParam("mail") String mail) {
        return "update";
    }

    @Path("/exception")
    @GET
    public String exceptionCase() {
        List<Object> plans;
        try {
            plans = getPlans();
        }catch (RuntimeException e) {
            return "Oops!";
        }

        return "Got it!";
    }

    @Path("/exception/variable/{id}")
    @GET
    public String exceptionCaseWithVariable(@PathParam("id") long id) {
        List<Object> plans;
        try {
            plans = getPlans();
        }catch (RuntimeException e) {
            return "Oops!";
        }

        return "Got it!";
    }

    @Path("/exception/variables/{id}/{id2}/{id3}")
    @GET
    public String exceptionCaseWithVariables(@PathParam("id") long id, @PathParam("id2") long id2, @PathParam("id3") long id3) {
        List<Object> plans;
        long totalId = 0;
        try {
            plans = getPlans();
            totalId += id3;
        }catch (RuntimeException e) {
            return "Oops!";
        }

        totalId += id + id2;

        return "Got it! " + totalId;
    }

    @Path("/exception/variable/trycatch/{id}")
    @GET
    public String exceptionCaseWithVariableAndNormalTryCatch(@PathParam("id") long id) {
        String someVariable = "Got it!";
        try {
            List<Object> plans = getPlans();
        }catch (RuntimeException e) {
            return "Oops!";
        }

        return someVariable;
    }

    private List<Object> getPlans() throws RuntimeException {
        return new ArrayList<>();
    }

    @Path("static")
    @GET
    public static void nothing(){
    }
}
