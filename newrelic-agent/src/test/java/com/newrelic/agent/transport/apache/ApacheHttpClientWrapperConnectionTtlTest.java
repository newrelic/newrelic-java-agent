/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.apache;

import com.newrelic.agent.transport.HttpClientWrapper;
import com.newrelic.agent.transport.ReadResult;
import com.newrelic.api.agent.Logger;
import org.apache.http.NoHttpResponseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class ApacheHttpClientWrapperConnectionTtlTest {
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 2_000;
    private static final long BETWEEN_REQUESTS_IN_MILLIS = 250;

    @Parameterized.Parameters(name = "{0}, ttl={1}ms, restorePooledConnection={4}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { Route.DIRECT, 0L, true, 1, false },
                { Route.DIRECT, 1_000L, true, 1, false },
                { Route.DIRECT, 50L, false, 2, false },
                { Route.DIRECT, 50L, false, 2, true },
                { Route.PROXY, 0L, true, 1, false },
                { Route.PROXY, 1_000L, true, 1, false },
                { Route.PROXY, 50L, false, 2, false },
                { Route.PROXY, 50L, false, 2, true }
        });
    }

    private final Route route;
    private final long connectionTtlInMillis;
    private final boolean expectStaleReuseFailure;
    private final int expectedConnectionCount;
    private final boolean restorePooledConnection;

    public ApacheHttpClientWrapperConnectionTtlTest(Route route, long connectionTtlInMillis,
            boolean expectStaleReuseFailure, int expectedConnectionCount, boolean restorePooledConnection) {
        this.route = route;
        this.connectionTtlInMillis = connectionTtlInMillis;
        this.expectStaleReuseFailure = expectStaleReuseFailure;
        this.expectedConnectionCount = expectedConnectionCount;
        this.restorePooledConnection = restorePooledConnection;
    }

    @Test
    public void expiresConnectionsBeforeTheyCanBeReused() throws Exception {
        try (RejectReusedConnectionServer server = new RejectReusedConnectionServer()) {
            ApacheProxyManager proxyManager = route == Route.PROXY
                    ? new ApacheProxyManager("127.0.0.1", server.getPort(), "http", null, null, mock(Logger.class))
                    : new ApacheProxyManager(null, null, null, null, null, mock(Logger.class));
            ApacheHttpClientWrapper client = new ApacheHttpClientWrapper(proxyManager, null,
                    REQUEST_TIMEOUT_IN_MILLIS, connectionTtlInMillis);

            try {
                ReadResult firstResult = client.execute(request(server), null);
                assertNotNull(firstResult);

                if (restorePooledConnection) {
                    client.beforeCheckpoint(null);
                    client.afterRestore(null);
                }

                Thread.sleep(BETWEEN_REQUESTS_IN_MILLIS);

                if (expectStaleReuseFailure) {
                    try {
                        client.execute(request(server), null);
                        fail("Expected the server to reject the reused connection without an HTTP response");
                    } catch (NoHttpResponseException expected) {
                        // The connection remained eligible for reuse, reproducing the stale connection failure.
                    }
                } else {
                    assertNotNull(client.execute(request(server), null));
                }

                assertEquals(expectedConnectionCount, server.getConnectionCount());
            } finally {
                client.shutdown();
            }
        }
    }

    private HttpClientWrapper.Request request(RejectReusedConnectionServer server) throws Exception {
        URL url = route == Route.PROXY
                ? new URL("http://collector.example.test/agent_listener/invoke_raw_method")
                : new URL("http://127.0.0.1:" + server.getPort() + "/agent_listener/invoke_raw_method");
        return new HttpClientWrapper.Request()
                .setURL(url)
                .setVerb(HttpClientWrapper.Verb.POST)
                .setEncoding("identity")
                .setData("{}".getBytes(StandardCharsets.UTF_8))
                .setRequestMetadata(Collections.<String, String>emptyMap());
    }

    private enum Route {
        DIRECT,
        PROXY
    }

    /**
     * Minimal HTTP endpoint/forward proxy that services one request per connection. If a client sends another request
     * on the same connection, it closes the socket without an HTTP response, matching the observable stale-reuse
     * failure while keeping the test deterministic and independent of external proxy software.
     */
    private static class RejectReusedConnectionServer implements Closeable {
        private static final byte[] RESPONSE = ("HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: 2\r\n"
                + "Connection: keep-alive\r\n"
                + "\r\n"
                + "{}").getBytes(StandardCharsets.US_ASCII);

        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final AtomicInteger connectionCount = new AtomicInteger();

        private RejectReusedConnectionServer() throws IOException {
            serverSocket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    acceptConnections();
                }
            });
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private int getConnectionCount() {
            return connectionCount.get();
        }

        private void acceptConnections() {
            while (!serverSocket.isClosed()) {
                try {
                    final Socket socket = serverSocket.accept();
                    connectionCount.incrementAndGet();
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            handleConnection(socket);
                        }
                    });
                } catch (IOException ignored) {
                    if (!serverSocket.isClosed()) {
                        throw new RuntimeException(ignored);
                    }
                }
            }
        }

        private void handleConnection(Socket socket) {
            try (Socket connection = socket;
                    BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                    BufferedOutputStream output = new BufferedOutputStream(connection.getOutputStream())) {
                if (!readRequest(input)) {
                    return;
                }
                output.write(RESPONSE);
                output.flush();

                // A second request means the client reused this connection. Consume it and close without responding.
                readRequest(input);
            } catch (IOException ignored) {
                // Client-side expiry closes the original socket while this handler is waiting for another request.
            }
        }

        private boolean readRequest(BufferedInputStream input) throws IOException {
            String requestLine = readLine(input);
            if (requestLine == null) {
                return false;
            }

            int contentLength = 0;
            String header;
            while ((header = readLine(input)) != null && !header.isEmpty()) {
                if (header.regionMatches(true, 0, "Content-Length:", 0, "Content-Length:".length())) {
                    contentLength = Integer.parseInt(header.substring("Content-Length:".length()).trim());
                }
            }
            for (int i = 0; i < contentLength; i++) {
                if (input.read() < 0) {
                    return false;
                }
            }
            return true;
        }

        private String readLine(BufferedInputStream input) throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int previous = -1;
            int current;
            while ((current = input.read()) >= 0) {
                if (previous == '\r' && current == '\n') {
                    byte[] bytes = line.toByteArray();
                    return new String(bytes, 0, Math.max(0, bytes.length - 1), StandardCharsets.US_ASCII);
                }
                line.write(current);
                previous = current;
            }
            return line.size() == 0 ? null : new String(line.toByteArray(), StandardCharsets.US_ASCII);
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
