/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.apache.ApacheHttpClientWrapper;
import com.newrelic.agent.transport.apache.ApacheProxyManager;
import com.newrelic.agent.transport.apache.ApacheSSLManager;
import com.newrelic.agent.transport.serverless.DataSenderServerless;
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

    public static DataSender createServerless(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessConfig serverlessConfig) {
        ServerlessWriter serverlessWriter = new ServerlessWriterImpl(logger, serverlessConfig.filePath());
        return new DataSenderServerless(config, logger, serverlessWriter);
    }

    public static DataSender create(DataSenderConfig config) {
        return DATA_SENDER_FACTORY.create(config);
    }

    public static DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener) {
        return DATA_SENDER_FACTORY.create(config, dataSenderListener);
    }

    private static class DefaultDataSenderFactory implements IDataSenderFactory {

        @Override
        public DataSender create(DataSenderConfig config) {
            return create(config, null);
        }

        @Override
        public DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener) {
            return new DataSenderImpl(
                    config,
                    buildApacheHttpClientWrapper(config, Agent.LOG),
                    dataSenderListener,
                    Agent.LOG,
                    ServiceFactory.getConfigService());
        }

        private ApacheHttpClientWrapper buildApacheHttpClientWrapper(DataSenderConfig config, Logger logger) {
            SSLContext sslContext = ApacheSSLManager.createSSLContext(config);

            ApacheProxyManager proxyManager = new ApacheProxyManager(
                    config.getProxyHost(),
                    config.getProxyPort(),
                    config.getProxyScheme(),
                    config.getProxyUser(),
                    config.getProxyPassword(),
                    logger);

            return new ApacheHttpClientWrapper(proxyManager, sslContext, config.getTimeoutInMilliseconds());
        }
    }

}
