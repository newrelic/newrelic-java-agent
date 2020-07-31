/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.util.Collection;

public interface TransactionEvent extends Event {

    float getDurationInSec();

    float getTotalTimeInSec();

    int getExternalCallCount();

    float getExternalDurationInSec();

    int getDatabaseCallCount();

    float getDatabaseDurationInSec();

    boolean isError();

    int getPort();

    String getName();

    String getTripId();

    Integer getMyPathHash();

    String getMyGuid();

    Integer getReferringPathHash();

    String getReferrerGuid();

    String getApdexPerfZone();

    String getMyAlternatePathHashes();
    
    String getParentType();
    
    String getParentApplicationId();
    
    String getParentAccountId();
    
    String getParentTransportType();

    Float getParentTransportDuration();

    String getParentId();
    
    Float priority();

    Boolean sampled();

    String getParentSpanId();

}
