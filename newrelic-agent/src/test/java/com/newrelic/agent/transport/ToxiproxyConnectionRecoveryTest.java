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
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.apache.http.ConnectionClosedException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * TCP fault injection around the embedded Jetty fixture. Jetty owns keep-alive behavior while Toxiproxy mutates
 * the established byte stream. Run with {@code ./gradlew :newrelic-agent:toxiproxyConnectionRecoveryTest}; the task
 * skips when Testcontainers cannot find Docker.
 */
@RunWith(Parameterized.class)
@SuppressWarnings("deprecation")
public class ToxiproxyConnectionRecoveryTest {
    private static final String RUN_PROPERTY = "newrelic.test.toxiproxy";
    private static final String CONTAINER_TESTS_ENABLED_PROPERTY = "newrelic.test.containers.enabled";
    private static final String TOXIPROXY_IMAGE = "ghcr.io/shopify/toxiproxy:2.12.0";
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 300;

    private static JettyCollectorFixture jetty;
    private static ToxiproxyContainer toxiproxy;
    private static ToxiproxyContainer.ContainerProxy proxy;
    private static boolean containersAvailable;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "RST invalidates a pooled connection and the next request recovers", Fault.RESET_PEER, 0L, 0L },
                { "silent blackhole still reaches socket timeout after TTL expiry", Fault.BLACKHOLE, 100L, 150L },
                { "downstream latency still reaches socket timeout after TTL expiry", Fault.LATENCY, 100L, 150L },
                { "truncated response invalidates the connection and the next request recovers", Fault.TRUNCATE,
                        0L, 0L }
        });
    }

    @BeforeClass
    public static void startContainers() throws Exception {
        if (!Boolean.getBoolean(RUN_PROPERTY)
                || !Boolean.parseBoolean(System.getProperty(CONTAINER_TESTS_ENABLED_PROPERTY, "true"))
                || !DockerClientFactory.instance().isDockerAvailable()) {
            return;
        }
        jetty = new JettyCollectorFixture();
        jetty.start();
        Testcontainers.exposeHostPorts(jetty.getFaultPort());
        toxiproxy = new ToxiproxyContainer(DockerImageName.parse(TOXIPROXY_IMAGE));
        toxiproxy.start();
        proxy = toxiproxy.getProxy("host.testcontainers.internal", jetty.getFaultPort());
        containersAvailable = true;
    }

    @AfterClass
    public static void stopContainers() throws Exception {
        if (toxiproxy != null) {
            toxiproxy.stop();
        }
        if (jetty != null) {
            jetty.stop();
        }
    }

    private final Fault fault;
    private final long connectionTtlInMillis;
    private final long delayBeforeFaultInMillis;

    public ToxiproxyConnectionRecoveryTest(String testName, Fault fault, long connectionTtlInMillis,
            long delayBeforeFaultInMillis) {
        this.fault = fault;
        this.connectionTtlInMillis = connectionTtlInMillis;
        this.delayBeforeFaultInMillis = delayBeforeFaultInMillis;
    }

    @Before
    public void requireContainers() {
        Assume.assumeTrue("Container tests are disabled or Docker is unavailable", containersAvailable);
    }

    @Test
    public void failsPredictablyAndRecoversOnANewConnection() throws Exception {
        ApacheHttpClientWrapper client = createClient();
        Toxic toxic = null;
        try {
            ReadResult firstResult = client.execute(request(), null);
            assertEquals(200, firstResult.getStatusCode());
            Thread.sleep(delayBeforeFaultInMillis);
            toxic = fault.apply(proxy);

            long startedAt = System.nanoTime();
            try {
                client.execute(request(), null);
                fail("Expected " + fault + " to fail the request");
            } catch (IOException expected) {
                assertTrue("Unexpected exception for " + fault + ": " + expected,
                        fault.matches(expected));
                if (fault.expectsSocketTimeout()) {
                    long elapsedInMillis = (System.nanoTime() - startedAt) / 1_000_000L;
                    assertTrue("Socket timeout returned too early: " + elapsedInMillis + "ms",
                            elapsedInMillis >= REQUEST_TIMEOUT_IN_MILLIS - 50L);
                }
            } finally {
                if (toxic != null) {
                    toxic.remove();
                    toxic = null;
                }
            }

            ReadResult recoveryResult = client.execute(request(), null);
            assertEquals(200, recoveryResult.getStatusCode());
            assertNotEquals(firstResult.getResponseBody(), recoveryResult.getResponseBody());
        } finally {
            if (toxic != null) {
                toxic.remove();
            }
            client.shutdown();
        }
    }

    private ApacheHttpClientWrapper createClient() {
        ApacheProxyManager proxyManager = new ApacheProxyManager(null, null, null, null, null, mock(Logger.class));
        return new ApacheHttpClientWrapper(proxyManager, null, REQUEST_TIMEOUT_IN_MILLIS,
                connectionTtlInMillis);
    }

    private HttpClientWrapper.Request request() throws Exception {
        URL url = new URL("http", proxy.getContainerIpAddress(), proxy.getProxyPort(), "/");
        return new HttpClientWrapper.Request()
                .setURL(url)
                .setVerb(HttpClientWrapper.Verb.POST)
                .setEncoding("identity")
                .setData("{}".getBytes(StandardCharsets.UTF_8))
                .setRequestMetadata(Collections.<String, String>emptyMap());
    }

    private enum Fault {
        RESET_PEER {
            @Override
            Toxic apply(ToxiproxyContainer.ContainerProxy containerProxy) throws IOException {
                return containerProxy.toxics().resetPeer("reset", ToxicDirection.DOWNSTREAM, 0L);
            }

            @Override
            boolean matches(IOException failure) {
                return hasCause(failure, NoHttpResponseException.class)
                        || hasCause(failure, SocketException.class);
            }
        },
        BLACKHOLE {
            @Override
            Toxic apply(ToxiproxyContainer.ContainerProxy containerProxy) throws IOException {
                return containerProxy.toxics().timeout("blackhole", ToxicDirection.DOWNSTREAM, 0L);
            }

            @Override
            boolean matches(IOException failure) {
                return hasCause(failure, SocketTimeoutException.class)
                        || hasCause(failure, NoHttpResponseException.class);
            }

            @Override
            boolean expectsSocketTimeout() {
                return true;
            }
        },
        LATENCY {
            @Override
            Toxic apply(ToxiproxyContainer.ContainerProxy containerProxy) throws IOException {
                return containerProxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 1_000L);
            }

            @Override
            boolean matches(IOException failure) {
                return hasCause(failure, SocketTimeoutException.class);
            }

            @Override
            boolean expectsSocketTimeout() {
                return true;
            }
        },
        TRUNCATE {
            @Override
            Toxic apply(ToxiproxyContainer.ContainerProxy containerProxy) throws IOException {
                return containerProxy.toxics().limitData("truncate", ToxicDirection.DOWNSTREAM, 64L);
            }

            @Override
            boolean matches(IOException failure) {
                return hasCause(failure, ConnectionClosedException.class)
                        || hasCause(failure, ClientProtocolException.class)
                        || hasCause(failure, NoHttpResponseException.class)
                        || hasCause(failure, SocketException.class);
            }
        };

        abstract Toxic apply(ToxiproxyContainer.ContainerProxy containerProxy) throws IOException;

        abstract boolean matches(IOException failure);

        boolean expectsSocketTimeout() {
            return false;
        }

        static boolean hasCause(Throwable failure, Class<? extends Throwable> expectedType) {
            Throwable current = failure;
            while (current != null) {
                if (expectedType.isInstance(current)) {
                    return true;
                }
                current = current.getCause();
            }
            return false;
        }
    }
}
