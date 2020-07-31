/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class MockCollector {

    private Server jettyServer;

    public MockCollector(int port, int sslPort) throws Exception {
        jettyServer = new Server();

        SslContextFactory sslContextFactory = new SslContextFactory("src/test/resources/keystore.jks");
        sslContextFactory.setKeyStorePassword("changeit");
        SslSocketConnector httpsConnector = new SslSocketConnector(sslContextFactory);
        httpsConnector.setPort(sslPort);

        Connector httpConnector = new SocketConnector();
        httpConnector.setPort(port);

        jettyServer.setConnectors(new Connector[] { httpConnector, httpsConnector });

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(MockCollectorServlet.class, "/*");
        jettyServer.setHandler(servletHandler);
        jettyServer.start();
    }

    public void stop() throws Exception {
        jettyServer.stop();
    }

}
