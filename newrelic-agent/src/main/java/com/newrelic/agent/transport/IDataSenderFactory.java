/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.transport.serverless.DataSenderServerlessConfig;

public interface IDataSenderFactory {

    DataSender createServerless(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessConfig serverlessConfig);

    DataSender create(DataSenderConfig config);

    DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener);

}
