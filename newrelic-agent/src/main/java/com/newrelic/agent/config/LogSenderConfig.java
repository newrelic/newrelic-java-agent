/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

/**
 * Configuration got LogSenderService
 */
public interface LogSenderConfig {

    boolean isEnabled();

    int getMaxSamplesStored();

}
