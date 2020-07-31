/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.core;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.service.Service;

public interface CoreService extends Service {

    InstrumentationProxy getInstrumentation();

    void shutdownAsync();
}
