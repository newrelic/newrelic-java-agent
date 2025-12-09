/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.transport.DataSender;
import com.newrelic.agent.transport.DataSenderListener;
import com.newrelic.agent.transport.IDataSenderFactory;
import com.newrelic.agent.transport.serverless.DataSenderServerlessConfig;
import com.newrelic.agent.transport.serverless.DataSenderServerlessImpl;
import com.newrelic.agent.transport.serverless.ServerlessWriter;
import com.newrelic.agent.transport.serverless.ServerlessWriterImpl;

public class MockDataSenderFactory implements IDataSenderFactory {

    private MockDataSender lastDataSender;

    @Override
    public DataSender createServerless(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessConfig serverlessConfig) {
        ServerlessWriter serverlessWriter = new ServerlessWriterImpl(logger, serverlessConfig.filePath());
        return new DataSenderServerlessImpl(config, logger, serverlessWriter);
    }

    @Override
    public DataSender create(DataSenderConfig config) {
        MockDataSender dataSender = new MockDataSender(config);
        lastDataSender = dataSender;
        return dataSender;
    }

    @Override
    public DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener) {
        MockDataSender dataSender = new MockDataSender(config, dataSenderListener);
        lastDataSender = dataSender;
        return dataSender;
    }

    public MockDataSender getLastDataSender() {
        return lastDataSender;
    }

}
