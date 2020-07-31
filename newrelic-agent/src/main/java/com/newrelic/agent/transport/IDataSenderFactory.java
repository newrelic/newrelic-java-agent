/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.config.DataSenderConfig;

public interface IDataSenderFactory {

    DataSender create(DataSenderConfig config);

    DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener);

}
