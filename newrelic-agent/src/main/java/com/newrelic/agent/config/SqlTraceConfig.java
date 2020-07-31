/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface SqlTraceConfig {

    /**
     * @return true if sql tracing is enabled.
     */
    boolean isEnabled();

    /**
     * @return true if using a longer sql id
     * The default SQL id is a 9-digit number but customers 
     * using DLP often scan outgoing traffic for 9-digit 
     * numbers (because they might be a SSN) and refuse to 
     * transmit the content. Enabling this feature causes the agent to 
     * use a 10-digit number for the id. 
     */
    boolean isUsingLongerSqlId();

}
