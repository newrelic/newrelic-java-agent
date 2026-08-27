/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.transport.apache.ApacheHttpClientWrapper;
import com.newrelic.agent.transport.apache.ApacheProxyManager;
import com.newrelic.api.agent.Logger;
import org.apache.http.NoHttpResponseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Embedded-Jetty tests for collector connection lifecycle behavior. Jetty supplies deterministic HTTP keep-alive,
 * idle closure, active abort, delayed response, and TLS behavior without requiring Docker.
 */
@RunWith(Parameterized.class)
public class JettyConnectionTtlTest {
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 2_000;

    private static JettyCollectorFixture jetty;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "reuses a healthy keep-alive connection", Endpoint.HTTP, "/", 0L, 50L,
                        Outcome.SAME_CONNECTION },
                { "TTL replaces a healthy connection before Jetty closes it", Endpoint.HTTP, "/", 25L, 100L,
                        Outcome.NEW_CONNECTION },
                { "Jetty idle close reproduces stale reuse without TTL", Endpoint.HTTP, "/", 0L, 350L,
                        Outcome.NO_HTTP_RESPONSE },
                { "TTL replaces a connection after Jetty idle close", Endpoint.HTTP, "/", 100L, 350L,
                        Outcome.NEW_CONNECTION },
                { "Jetty response close prevents pooling", Endpoint.HTTP, "/close", 0L, 0L,
                        Outcome.NEW_CONNECTION },
                { "TTL does not mask an active request abort", Endpoint.HTTP, "/abort", 100L, 0L,
                        Outcome.ACTIVE_ABORT },
                { "active request can outlive TTL", Endpoint.HTTP, "/slow", 100L, 0L,
                        Outcome.ACTIVE_REQUEST_OUTLIVES_TTL },
                { "repeated idle closes recover when TTL is shorter", Endpoint.HTTP, "/", 100L, 350L,
                        Outcome.RECOVERY_SOAK },
                { "TLS connection is reused before TTL", Endpoint.HTTPS, "/", 1_000L, 50L,
                        Outcome.SAME_CONNECTION },
                { "TLS idle close reproduces stale reuse without TTL", Endpoint.HTTPS, "/", 0L, 350L,
                        Outcome.NO_HTTP_RESPONSE },
                { "TTL replaces a TLS connection after Jetty idle close", Endpoint.HTTPS, "/", 100L, 350L,
                        Outcome.NEW_CONNECTION }
        });
    }

    @BeforeClass
    public static void startJetty() throws Exception {
        jetty = new JettyCollectorFixture();
        jetty.start();
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        if (jetty != null) {
            jetty.stop();
        }
    }

    private final Endpoint endpoint;
    private final String path;
    private final long connectionTtlInMillis;
    private final long delayBetweenRequestsInMillis;
    private final Outcome outcome;

    public JettyConnectionTtlTest(String testName, Endpoint endpoint, String path, long connectionTtlInMillis,
            long delayBetweenRequestsInMillis, Outcome outcome) {
        this.endpoint = endpoint;
        this.path = path;
        this.connectionTtlInMillis = connectionTtlInMillis;
        this.delayBetweenRequestsInMillis = delayBetweenRequestsInMillis;
        this.outcome = outcome;
    }

    @Test
    public void handlesConnectionLifecycleScenario() throws Exception {
        ApacheHttpClientWrapper client = createClient(connectionTtlInMillis, endpoint);
        try {
            if (outcome == Outcome.ACTIVE_ABORT) {
                try {
                    client.execute(request(path), null);
                    fail("Expected Jetty to abort the request without an HTTP response");
                } catch (NoHttpResponseException expected) {
                    // Expected: Jetty closed the active connection before returning an HTTP response.
                }
                return;
            }

            String firstClientPort = executeAndGetClientPort(client, path);

            if (outcome == Outcome.ACTIVE_REQUEST_OUTLIVES_TTL) {
                String secondClientPort = executeAndGetClientPort(client, "/");
                assertNotEquals(firstClientPort, secondClientPort);
                return;
            }

            if (outcome == Outcome.RECOVERY_SOAK) {
                String previousClientPort = firstClientPort;
                for (int attempt = 0; attempt < 10; attempt++) {
                    Thread.sleep(delayBetweenRequestsInMillis);
                    String nextClientPort = executeAndGetClientPort(client, path);
                    assertNotEquals(previousClientPort, nextClientPort);
                    previousClientPort = nextClientPort;
                }
                return;
            }

            Thread.sleep(delayBetweenRequestsInMillis);

            if (outcome == Outcome.NO_HTTP_RESPONSE) {
                try {
                    client.execute(request(path), null);
                    fail("Expected Jetty's idle connection close to produce NoHttpResponseException on reuse");
                } catch (NoHttpResponseException expected) {
                    // Expected: Apache leases the connection before its default stale validation threshold.
                }
                return;
            }

            String secondClientPort = executeAndGetClientPort(client, path);
            if (outcome == Outcome.SAME_CONNECTION) {
                assertEquals(firstClientPort, secondClientPort);
            } else {
                assertNotEquals(firstClientPort, secondClientPort);
            }
        } finally {
            client.shutdown();
        }
    }

    private ApacheHttpClientWrapper createClient(long ttlInMillis, Endpoint targetEndpoint) throws Exception {
        ApacheProxyManager proxyManager = new ApacheProxyManager(null, null, null, null, null, mock(Logger.class));
        return new ApacheHttpClientWrapper(proxyManager, targetEndpoint.createSslContext(),
                REQUEST_TIMEOUT_IN_MILLIS, ttlInMillis);
    }

    private String executeAndGetClientPort(ApacheHttpClientWrapper client, String requestPath) throws Exception {
        ReadResult result = client.execute(request(requestPath), null);
        assertEquals(200, result.getStatusCode());
        return result.getResponseBody();
    }

    private HttpClientWrapper.Request request(String requestPath) throws Exception {
        return new HttpClientWrapper.Request()
                .setURL(jetty.url(endpoint.tls, requestPath))
                .setVerb(HttpClientWrapper.Verb.POST)
                .setEncoding("identity")
                .setData("{}".getBytes(StandardCharsets.UTF_8))
                .setRequestMetadata(Collections.<String, String>emptyMap());
    }

    private enum Outcome {
        SAME_CONNECTION,
        NEW_CONNECTION,
        NO_HTTP_RESPONSE,
        ACTIVE_ABORT,
        ACTIVE_REQUEST_OUTLIVES_TTL,
        RECOVERY_SOAK
    }

    private enum Endpoint {
        HTTP(false),
        HTTPS(true);

        private final boolean tls;

        Endpoint(boolean tls) {
            this.tls = tls;
        }

        private SSLContext createSslContext() throws Exception {
            if (!tls) {
                return null;
            }
            TrustManager[] trustManagers = { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
            } };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        }
    }
}
