package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.HttpTestServer;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class HttpServerLocatorTest {

    @Test
    public void testHttpServerLocator() throws IOException {
        HttpTestServer httpServer = HttpServerLocator.createAndStart();

        assertThat(httpServer, instanceOf(HttpTestServer.class));
    }
}