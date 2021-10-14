/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Application;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.glassfish", "com.nr.instrumentation.javax.ws" })
public class SubResourceTest extends JerseyTest {

    @Test
    public void testGetById() {
        final String id = target("customers/type-id/1").request().get(String.class);
        assertEquals("one", id);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/customers/type-{type}/{id} (GET)", transactionName);
    }

    @Test
    public void testGetAllById() {
        final String id = target("customers/type-id").request().get(String.class);
        assertEquals("{ \"items\": [ \"one\", \"two\", \"three\" ] }", id);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/customers/type-{type} (GET)", transactionName);
    }

    @Test
    public void testGetByFirstLast() {
        final String id = target("customers/type-firstLast/first-last").request().get(String.class);
        assertEquals("FirstName, LastName", id);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/customers/type-{type}/{first}-{last} (GET)", transactionName);
    }

    @Test
    public void testGetAllByFirstLast() {
        final String id = target("customers/type-firstLast").request().get(String.class);
        assertEquals("{ \"names\": [ \"FirstName, LastName\", \"SecondName, LastName\", \"ThirdName, LastName\" ] }", id);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/customers/type-{type} (GET)", transactionName);
    }

    @Test
    public void testDoubleNestedGetById() {
        final String id = target("customers/orders/getStuff/1").request().get(String.class);
        assertEquals("one", id);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/customers/orders/getStuff/{id} (GET)", transactionName);
    }

    @Test
    public void testDoubleNestedGetAllById() {
        final String id = target("customers/orders/getStuff").request().get(String.class);
        assertEquals("{ \"items\": [ \"one\", \"two\", \"three\" ] }", id);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(1, finishedTransactionCount);
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/customers/orders/getStuff (GET)", transactionName);
    }

    @Test
    public void testDefaultGet() {
        final String result = target("customers/default-get-test").request().get(String.class);
        assertEquals("TEST", result);
    }

    @Test
    public void getGetInterfaceWithImpl() {
        for (int i = 0; i < 100; i++) {
            // We need to run this many times in order to get the path to be re-used incorrectly
            final String result = target("v1/people/getPeople").request().get(String.class);
            assertEquals("Returning from getPeople", result);
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(30000);
        assertEquals(100, finishedTransactionCount);
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertEquals("WebTransaction/RestWebService/v1/people/getPeople (GET)", transactionName);
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(CustomerLocatorResource.class, FirstLastSubResource.class, IdSubResource.class, PeopleResource.class);
    }

}
