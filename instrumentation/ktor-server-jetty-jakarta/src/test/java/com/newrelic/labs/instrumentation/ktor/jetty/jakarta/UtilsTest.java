package com.newrelic.labs.instrumentation.ktor.jetty.jakarta;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void getTransactionName_withPathAndMethod() {
        assertEquals("api/users - {GET}", Utils.getTransactionName("/api/users", "GET"));
    }

    @Test
    public void getTransactionName_withRootUri() {
        assertEquals("Root - {POST}", Utils.getTransactionName("/", "POST"));
    }

    @Test
    public void getTransactionName_withNullUri() {
        assertEquals("Unknown - {GET}", Utils.getTransactionName(null, "GET"));
    }

    @Test
    public void getTransactionName_withNullMethod() {
        assertEquals("path", Utils.getTransactionName("/path", null));
    }

    @Test
    public void getTransactionName_withEmptyUri() {
        assertEquals("Root - {DELETE}", Utils.getTransactionName("", "DELETE"));
    }
}
