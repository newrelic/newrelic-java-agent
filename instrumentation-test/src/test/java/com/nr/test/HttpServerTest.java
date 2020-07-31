/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import com.newrelic.agent.introspec.internal.HttpServerRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.http.HttpRequestClass;
import com.newrelic.agent.deps.org.apache.http.client.methods.CloseableHttpResponse;
import com.newrelic.agent.deps.org.apache.http.client.methods.HttpUriRequest;
import com.newrelic.agent.deps.org.apache.http.client.methods.RequestBuilder;
import com.newrelic.agent.deps.org.apache.http.impl.client.CloseableHttpClient;
import com.newrelic.agent.deps.org.apache.http.impl.client.HttpClientBuilder;
import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.internal.HttpServerLocator;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.example.http")
public class HttpServerTest {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testServerUp() throws IOException, URISyntaxException {
        URI uri = server.getEndPoint();

        HttpUriRequest request = RequestBuilder.get().setUri(uri).build();
        CloseableHttpClient connection = HttpClientBuilder.create().build();
        CloseableHttpResponse response = connection.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testExternalCall() throws IOException, URISyntaxException {
        Introspector intro = InstrumentationTestRunner.getIntrospector();
        HttpRequestClass myClass = new HttpRequestClass();
        myClass.performHttp(server.getEndPoint());

        assertEquals(2, intro.getFinishedTransactionCount());
        Collection<String> names = intro.getTransactionNames();
        assertEquals(2, names.size());
        String txName1 = "OtherTransaction/Custom/com.example.http.HttpRequestClass/performHttp";
        String txName2 = "WebTransaction/Custom/ExternalHTTPServer";
        assertTrue(names.contains(txName1));
        assertTrue(names.contains(txName2));
        Collection<ExternalRequest> externals = intro.getExternalRequests(txName1);
        assertEquals(1, externals.size());
        ExternalRequest req = externals.iterator().next();
        assertEquals(1, req.getCount());
        assertEquals("localhost", req.getHostname());
        assertEquals("NewRelicApacheHttp", req.getLibrary());
    }

    @Test
    public void testCatCallSetName() throws IOException, URISyntaxException {
        Introspector intro = InstrumentationTestRunner.getIntrospector();
        HttpRequestClass myClass = new HttpRequestClass();
        myClass.performCatHttp(server.getEndPoint());

        assertEquals(2, intro.getFinishedTransactionCount());
        Collection<String> names = intro.getTransactionNames();
        assertEquals(2, names.size());
        String txName1 = "OtherTransaction/Custom/com.example.http.HttpRequestClass/performCatHttp";
        String txName2 = "WebTransaction/Custom/ExternalHTTPServer";
        assertTrue(names.contains(txName1));
        assertTrue(names.contains(txName2));
        Collection<ExternalRequest> externals = intro.getExternalRequests(txName1);
        assertEquals(1, externals.size());
        ExternalRequest req = externals.iterator().next();
        assertEquals(1, req.getCount());
        assertEquals("localhost", req.getHostname());
        CatHelper.verifyOneSuccessfulCat(intro, txName1);
    }

    // this one creates a new path hash at the end
    @Test
    public void testCatCallDefaultName() throws IOException, URISyntaxException {
        Introspector intro = InstrumentationTestRunner.getIntrospector();
        HttpRequestClass myClass = new HttpRequestClass();
        myClass.performCatHttpNoSetName(server.getEndPoint());

        assertEquals(2, intro.getFinishedTransactionCount());
        Collection<String> names = intro.getTransactionNames();
        assertEquals(2, names.size());
        String txName1 = "OtherTransaction/Custom/com.example.http.HttpRequestClass/performCatHttpNoSetName";
        String txName2 = "WebTransaction/Custom/ExternalHTTPServer";
        assertTrue(names.contains(txName1));
        assertTrue(names.contains(txName2));
        Collection<ExternalRequest> externals = intro.getExternalRequests(txName1);
        assertEquals(1, externals.size());
        ExternalRequest req = externals.iterator().next();
        assertEquals(1, req.getCount());
        assertEquals("localhost", req.getHostname());
        CatHelper.verifyOneSuccessfulCat(intro, txName1);
    }
}
