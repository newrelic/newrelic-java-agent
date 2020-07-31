/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

/**
 * Represents an external request made and traced.
 */
public interface ExternalRequest {
    String getHostname();

    int getCount();

    String getLibrary();

    String getOperation();

    String getTransactionGuild();

    String getMetricName();

}
