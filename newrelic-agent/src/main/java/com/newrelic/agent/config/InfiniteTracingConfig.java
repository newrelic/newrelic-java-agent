/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface InfiniteTracingConfig {
    String getTraceObserverHost();

    int getTraceObserverPort();

    int getSpanEventsQueueSize();

    Double getFlakyPercentage();

    Long getFlakyCode();

    boolean getUsePlaintext();

    boolean getUseCompression();

    boolean getUseBatching();

    boolean isEnabled();

}
