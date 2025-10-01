/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.service.Service;

public interface BrowserService extends Service {

    /**
     * Get the browser configuration for the application.
     */
    BrowserConfig getBrowserConfig(String appName);

}
