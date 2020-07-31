/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface KeyTransactionConfig {

    /**
     * Has the given transaction name been configured as a Key Transaction with an ApdexT?
     */
    boolean isApdexTSet(String transactionName);

    /**
     * If NewRelic sent an ApdexT for a key transaction, return that; otherwise, return the ApdextT value for the
     * application.
     */
    long getApdexTInMillis(String transactionName);
}
