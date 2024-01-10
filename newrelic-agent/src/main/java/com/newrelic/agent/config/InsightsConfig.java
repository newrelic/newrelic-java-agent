/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface InsightsConfig {

    boolean isEnabled();

    int getMaxSamplesStored();

    /**
     * Returns the max attribute size.
     * @since 9.0.0
     */
    int getMaxAttributeValue();

}
