/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import java.util.Map;

/**
 * Implementations of this interface must be thread safe
 */
public interface BrowserTransactionState {

    long getDurationInMilliseconds();

    long getExternalTimeInMilliseconds();

    String getBrowserTimingHeader();

    String getBrowserTimingHeader(String nonce);

    String getBrowserTimingHeaderForJsp();

    String getTransactionName();

    Map<String, Object> getUserAttributes();

    Map<String, Object> getAgentAttributes();

    String getAppName();

}
