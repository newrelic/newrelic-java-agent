/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface CrossProcessConfig {

    String getCrossProcessId();

    String getApplicationId();

    String getEncodedCrossProcessId();

    String getEncodingKey();

    String getSyntheticsEncodingKey();

    boolean isTrustedAccountId(String accountId);

    /**
     * The agent identifies itself to other processes in external call request headers if this is enabled.
     *
     * @return true if cross process feature is enabled
     */
    boolean isCrossApplicationTracing();

}
