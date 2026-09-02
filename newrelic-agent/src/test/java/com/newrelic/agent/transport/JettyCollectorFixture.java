/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class JettyCollectorFixture {
    private static final int CONNECTION_IDLE_TIMEOUT_IN_MILLIS = 200;
    // TLS handshakes can exceed the deliberately tight HTTP timeout under CI load.
    private static final int TLS_CONNECTION_IDLE_TIMEOUT_IN_MILLIS = 1_000;
    private static final int FAULT_TEST_IDLE_TIMEOUT_IN_MILLIS = 2_000;

    private final Server server = new Server();
    private final SocketConnector httpConnector = connector(CONNECTION_IDLE_TIMEOUT_IN_MILLIS);
    private final SslSocketConnector httpsConnector = sslConnector(TLS_CONNECTION_IDLE_TIMEOUT_IN_MILLIS);
    private final SocketConnector faultConnector = connector(FAULT_TEST_IDLE_TIMEOUT_IN_MILLIS);

    void start() throws Exception {
        server.setConnectors(new Connector[] { httpConnector, httpsConnector, faultConnector });
        server.setHandler(new CollectorHandler());
        server.start();
    }

    void stop() throws Exception {
        server.stop();
        server.join();
    }

    URL url(boolean tls, String path) throws Exception {
        return new URL(tls ? "https" : "http", "localhost",
                tls ? httpsConnector.getLocalPort() : httpConnector.getLocalPort(), path);
    }

    int getFaultPort() {
        return faultConnector.getLocalPort();
    }

    private static SocketConnector connector(int idleTimeoutInMillis) {
        SocketConnector connector = new SocketConnector();
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        connector.setMaxIdleTime(idleTimeoutInMillis);
        return connector;
    }

    private static SslSocketConnector sslConnector(int idleTimeoutInMillis) {
        URL keyStore = JettyCollectorFixture.class.getClassLoader().getResource("keystore.jks");
        if (keyStore == null) {
            throw new IllegalStateException("Unable to find test keystore.jks");
        }
        SslContextFactory sslContextFactory = new SslContextFactory(keyStore.toExternalForm());
        sslContextFactory.setKeyStorePassword("changeit");
        SslSocketConnector connector = new SslSocketConnector(sslContextFactory);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        connector.setMaxIdleTime(idleTimeoutInMillis);
        return connector;
    }

    private static class CollectorHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                HttpServletResponse response) throws IOException, ServletException {
            baseRequest.setHandled(true);

            if ("/abort".equals(target)) {
                AbstractHttpConnection.getCurrentConnection().getEndPoint().close();
                return;
            }

            if ("/slow".equals(target)) {
                try {
                    Thread.sleep(350L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while delaying response", interrupted);
                }
            }

            if ("/close".equals(target)) {
                response.setHeader("Connection", "close");
            }

            byte[] body = Integer.toString(request.getRemotePort()).getBytes(StandardCharsets.UTF_8);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        }
    }
}
