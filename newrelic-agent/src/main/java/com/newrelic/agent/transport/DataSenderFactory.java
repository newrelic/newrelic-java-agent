/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.Agent;
import com.newrelic.agent.serverless.ServerlessService;
import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.apache.ApacheHttpClientWrapper;
import com.newrelic.agent.transport.apache.ApacheProxyManager;
import com.newrelic.agent.transport.apache.ApacheSSLManager;
import com.newrelic.agent.transport.serverless.DataSenderServerlessImpl;
import com.newrelic.agent.transport.serverless.DataSenderServerlessConfig;
import com.newrelic.agent.transport.serverless.ServerlessWriterImpl;
import com.newrelic.agent.transport.serverless.ServerlessWriter;
import com.newrelic.api.agent.Logger;

import javax.net.ssl.SSLContext;

public class DataSenderFactory {

    private static volatile IDataSenderFactory DATA_SENDER_FACTORY = new DefaultDataSenderFactory();

    private DataSenderFactory() {
    }

    public static void setDataSenderFactory(IDataSenderFactory dataSenderFactory) {
        if (dataSenderFactory == null) {
            return;
        }
        DATA_SENDER_FACTORY = dataSenderFactory;
    }

    /**
     * For testing.
     */
    public static IDataSenderFactory getDataSenderFactory() {
        return DATA_SENDER_FACTORY;
    }

    public static DataSender createServerless(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessService serverlessService, ServerlessConfig serverlessConfig) {
        return DATA_SENDER_FACTORY.createServerless(config, logger, serverlessService, serverlessConfig);
    }

    public static DataSender create(DataSenderConfig config) {
        return DATA_SENDER_FACTORY.create(config);
    }

    public static DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener) {
        return DATA_SENDER_FACTORY.create(config, dataSenderListener);
    }

    private static class DefaultDataSenderFactory implements IDataSenderFactory {

        @Override
        public DataSender createServerless(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessService serverlessService, ServerlessConfig serverlessConfig) {
            ServerlessWriter serverlessWriter = new ServerlessWriterImpl(logger, serverlessConfig.filePath());
            return new DataSenderServerlessImpl(config, logger, serverlessService, serverlessWriter);
        }

        @Override
        public DataSender create(DataSenderConfig config) {
            return create(config, null);
        }

        @Override
        public DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener) {
            return new DataSenderImpl(
                    config,
                    createHttpClientWrapper(config, Agent.LOG),
                    dataSenderListener,
                    Agent.LOG,
                    ServiceFactory.getConfigService());
        }
    }

    /**
     * Creates an HTTP client that honors the agent's proxy, CA bundle, and timeout configuration, defaulting
     * every request's Content-Type to {@code application/json} for the New Relic collector protocol.
     */
    public static HttpClientWrapper createHttpClientWrapper(DataSenderConfig config, Logger logger) {
        return createHttpClientWrapper(config, logger, true);
    }

    /**
     * Creates an HTTP client that honors the agent's proxy, CA bundle, and timeout configuration.
     *
     * @param setDefaultJsonContentTypeHeader whether every request should default to
     *      {@code Content-Type: application/json}. Pass {@code false} for a client whose requests set their own
     *      Content-Type (e.g. OTLP export's binary protobuf payloads).
     */
    public static HttpClientWrapper createHttpClientWrapper(DataSenderConfig config, Logger logger,
            boolean setDefaultJsonContentTypeHeader) {
        SSLContext sslContext = ApacheSSLManager.createSSLContext(config);

        ApacheProxyManager proxyManager = new ApacheProxyManager(
                config.getProxyHost(),
                config.getProxyPort(),
                config.getProxyScheme(),
                config.getProxyUser(),
                config.getProxyPassword(),
                logger);

        return new ApacheHttpClientWrapper(proxyManager, sslContext, config.getTimeoutInMilliseconds(),
                config.getCollectorConnectionTtlInMilliseconds(), setDefaultJsonContentTypeHeader);
    }

}
