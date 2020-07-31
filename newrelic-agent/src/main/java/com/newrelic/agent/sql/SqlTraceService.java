/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.service.Service;

/**
 * The interface for the sql service.
 */
public interface SqlTraceService extends Service {

    SlowQueryListener getSlowQueryListener(String appName);

}