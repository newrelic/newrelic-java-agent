/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

public interface TransactionProfileService {

    boolean isTransactionProfileSessionActive();
    
    TransactionProfileSession getTransactionProfileSession();
}
