/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.integration;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.HttpIntegrationServerConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class HttpIntegrationServerService extends AbstractService implements AgentConfigListener, ExtendedTransactionListener, HarvestListener {
    private static final String HEALTH_CHECK_CONTEXT = "/_nr_/health";
    private static final String FORCE_RESTART_CONTEXT = "/_nr_/restart";

    private HttpServer server = null;
    private final HttpIntegrationServerConfig httpIntegrationServerConfig;
    private static int transactionsFinishedCount = 0;
    private static int transactionsCancelledCount = 0;
    private static long lastHarvestTimestamp = 0;

    public HttpIntegrationServerService(AgentConfig defaultAgentConfig) {
        super(HttpIntegrationServerService.class.getSimpleName());
        this.httpIntegrationServerConfig = defaultAgentConfig.getHttpIntegrationServerConfig();
    }

    @Override
    protected void doStart() {
        if (isEnabled()) {
            ServiceFactory.getTransactionService().addTransactionListener(this);
            ServiceFactory.getHarvestService().addHarvestListener(this);

            Agent.LOG.log(Level.INFO, "Starting HTTP integration server on port {0}", httpIntegrationServerConfig.getPort());

            server = configureIntegrationServer();
            if (server != null) {
                server.start();
            }
        }
    }

    @Override
    protected void doStop() {
        if (isEnabled()) {
            if (server != null) {
                server.stop(0);
                server = null;
            }

            ServiceFactory.getTransactionService().removeTransactionListener(this);
            ServiceFactory.getHarvestService().removeHarvestListener(this);
        }
    }

    @Override
    public boolean isEnabled() {
        return httpIntegrationServerConfig.isEnabled();
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        boolean newEnabledFlag = agentConfig.getHttpIntegrationServerConfig().isEnabled();

        if (newEnabledFlag != httpIntegrationServerConfig.isEnabled()) {
            Agent.LOG.log(Level.INFO, "HTTP Integration Server enabled flag changed to {0}", newEnabledFlag);
            httpIntegrationServerConfig.setEnabled(newEnabledFlag);

            if (newEnabledFlag) {
                doStart();
            } else {
                doStop();
            }
        }
    }

    private HttpServer configureIntegrationServer() {
        HttpServer server;
        InetAddress localHost = InetAddress.getLoopbackAddress();
        InetSocketAddress socketAddress = new InetSocketAddress(localHost, httpIntegrationServerConfig.getPort());

        try {
            server = HttpServer.create(socketAddress, 1);
            HttpContext healthCheckContext = server.createContext(HEALTH_CHECK_CONTEXT);
            HttpContext forceRestartContext = server.createContext(FORCE_RESTART_CONTEXT);

            healthCheckContext.setHandler(new HealthCheckHttpHandler());
            forceRestartContext.setHandler(new ForceRestartHttpHandler());
        } catch (IOException e) {
            Agent.LOG.log(Level.INFO, "HTTP Integration Server threw an exception during configuration", e);
            return null;
        }

        return server;
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        //no-op
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
        transactionsCancelledCount++;
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        transactionsFinishedCount++;
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        lastHarvestTimestamp = System.currentTimeMillis();
    }

    @Override
    public void afterHarvest(String appName) {
        //no-op
    }

    private static class HealthCheckHttpHandler implements HttpHandler {
        @Override
        @SuppressWarnings("unchecked")
        public void handle(HttpExchange exchange) throws IOException {
            // We don't really need any info from the request, since the endpoint is the only trigger we need
            JSONObject healthResponse = new JSONObject();
            healthResponse.put("transactionsFinished", transactionsFinishedCount);
            healthResponse.put("transactionsCancelled", transactionsCancelledCount);
            healthResponse.put("lastHarvest", lastHarvestTimestamp);

            try {
                OutputStream outputStream = exchange.getResponseBody();
                String healthResponseAsJsonString = healthResponse.toString();
                exchange.getResponseHeaders().add("Content-Type", "application/json");

                exchange.sendResponseHeaders(200, healthResponseAsJsonString.length());
                outputStream.write(healthResponseAsJsonString.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                Agent.LOG.log(Level.INFO, "HTTP Integration Server threw exception while handling health check request", e);
            }
        }
    }

    private static class ForceRestartHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            IRPMService rpmService = ServiceFactory.getRPMService();
            rpmService.reconnect();

            try {
                OutputStream outputStream = exchange.getResponseBody();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                Agent.LOG.log(Level.INFO, "HTTP Integration Server threw exception while handling health check force restart request", e);
            }
        }
    }
}
