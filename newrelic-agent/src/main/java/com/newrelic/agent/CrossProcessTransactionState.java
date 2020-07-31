/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.CrossProcessState;

public interface CrossProcessTransactionState extends CrossProcessState {

    /**
     * Send the transaction data response header.
     */
    void writeResponseHeaders();

    /**
     * An id that is consistent across the entire trip through the system, or null if no trip id is required. For root
     * transaction, same as guid.
     * 
     * @return the trip id or null if one is not needed. The logic behind "not required" is complex.
     */
    String getTripId();

    /**
     * Combination of transactions that were called including this one. Based on the current transaction name.
     */
    int generatePathHash();

    /**
     * A sorted, comma-separated list of alternate pathHash values that were given to callees, excluding the final
     * pathHash.
     * 
     */
    String getAlternatePathHashes();

}
