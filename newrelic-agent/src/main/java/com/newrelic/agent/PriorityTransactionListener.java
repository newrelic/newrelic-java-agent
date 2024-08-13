/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * Allows objects to be notified when transactions finish. Can be used to prioritize the order
 * that listeners are notified.
 */
public interface PriorityTransactionListener extends TransactionListener {

//    void setPriority(int order);
//    int getPriority();

}
