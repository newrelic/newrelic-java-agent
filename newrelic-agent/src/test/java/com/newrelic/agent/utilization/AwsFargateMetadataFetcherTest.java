/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.config.AwsConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsFargateMetadataFetcherTest {

    private HttpServer server;
    private String testUrl;
    private AtomicBoolean requestReceived;

    @Before
    public void setup() throws IOException {
        requestReceived = new AtomicBoolean(false);
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/", exchange -> {
            requestReceived.set(true);
            String response = "ok";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        server.start();
        testUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test(expected = IOException.class)
    public void testProxyEnabled_ProxyCausesFailure() throws IOException {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "65000"); // closed port

        try {
            String url = "http://169.254.170.2/v4/task";
            AwsConfig awsConfig = mock(AwsConfig.class);
            when(awsConfig.isFargateMetadataProxyBypassEnabled()).thenReturn(false);

            AwsFargateMetadataFetcher awsFargateMetadataFetcher = new AwsFargateMetadataFetcher(url, awsConfig);
            awsFargateMetadataFetcher.openStream();

        } finally {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

    @Test
    public void testProxyDisabled_IgnoresProxyAndSucceeds() throws Exception {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "65000"); // closed port

        try {
            AwsConfig awsConfig = mock(AwsConfig.class);
            when(awsConfig.isFargateMetadataProxyBypassEnabled()).thenReturn(true);

            AwsFargateMetadataFetcher awsFargateMetadataFetcher = new AwsFargateMetadataFetcher(testUrl, awsConfig);
            InputStream inputStream = awsFargateMetadataFetcher.openStream();
            String body = readBody(inputStream);

            assertEquals("ok", body);
            assertTrue(requestReceived.get());
        } finally {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

    private String readBody(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.readLine();
        }
    }
}
