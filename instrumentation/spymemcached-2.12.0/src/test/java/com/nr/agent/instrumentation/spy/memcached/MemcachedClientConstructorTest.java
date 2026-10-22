/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import net.spy.memcached.ClientMode;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Runs against the AWS ElastiCache Cluster Client, which stores node addresses as the resolved IP
 * only. Each public constructor must still report the configured host, not the IP or its reverse-DNS name. The AWS
 * client's (List) and varargs constructors call a private constructor directly rather than the
 * (ConnectionFactory, List) one, so every one of them needs to be woven.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.nr.agent.instrumentation.spy.memcached")
public class MemcachedClientConstructorTest {

    private static final String CONFIGURED_HOST = "memcached.test";
    private static final byte[] LOOPBACK = new byte[] { 127, 0, 0, 1 };

    private static FakeMemcachedServer server;

    @BeforeClass
    public static void startServer() throws IOException {
        server = new FakeMemcachedServer();
    }

    @AfterClass
    public static void stopServer() throws IOException {
        server.close();
    }

    @Before
    @After
    public void clearIntrospector() {
        InstrumentationTestRunner.getIntrospector().clear();
    }

    @Test
    public void listConstructorReportsConfiguredHost() throws Exception {
        MemcachedClient client = new MemcachedClient(configuredAddresses());
        assertConfiguredHostReported(client);
    }

    @Test
    public void varargsConstructorReportsConfiguredHost() throws Exception {
        MemcachedClient client = new MemcachedClient(configuredAddresses().get(0));
        assertConfiguredHostReported(client);
    }

    @Test
    public void connectionFactoryConstructorReportsConfiguredHost() throws Exception {
        ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder();
        builder.setClientMode(ClientMode.Static);
        MemcachedClient client = new MemcachedClient(builder.build(), configuredAddresses());
        assertConfiguredHostReported(client);
    }

    private static List<InetSocketAddress> configuredAddresses() throws IOException {
        InetAddress address = InetAddress.getByAddress(CONFIGURED_HOST, LOOPBACK);
        return Collections.singletonList(new InetSocketAddress(address, server.getPort()));
    }

    private void assertConfiguredHostReported(MemcachedClient client) throws Exception {
        try {
            getInTransaction(client);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            assertTrue(introspector.getFinishedTransactionCount(5000) > 0);
            String expected = "Datastore/instance/Memcached/" + CONFIGURED_HOST + "/" + server.getPort();
            assertTrue("Expected " + expected + " but found " + introspector.getUnscopedMetrics().keySet(),
                    introspector.getUnscopedMetrics().containsKey(expected));
        } finally {
            client.shutdown(1, TimeUnit.SECONDS);
        }
    }

    @Trace(dispatcher = true)
    private void getInTransaction(MemcachedClient client) {
        // Synchronous, so the datastore metrics are recorded before the transaction ends
        client.get("key");
    }

    private static class FakeMemcachedServer implements AutoCloseable {
        private final ServerSocket serverSocket;

        FakeMemcachedServer() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getByAddress(LOOPBACK));
            Thread acceptor = new Thread(this::acceptLoop, "fake-memcached-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    final Socket socket = serverSocket.accept();
                    Thread handler = new Thread(() -> handle(socket), "fake-memcached-handler");
                    handler.setDaemon(true);
                    handler.start();
                } catch (IOException e) {
                    return;
                }
            }
        }

        private void handle(Socket socket) {
            try (Socket s = socket;
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
                    OutputStream out = s.getOutputStream()) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("set ")) {
                        in.readLine();
                        out.write("STORED\r\n".getBytes(StandardCharsets.ISO_8859_1));
                    } else {
                        out.write("END\r\n".getBytes(StandardCharsets.ISO_8859_1));
                    }
                    out.flush();
                }
            } catch (IOException ignored) {
                // client went away
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }
}
