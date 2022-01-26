/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public class ConfigConstant {
    public static final int MAX_USER_ATTRIBUTES = 64;
    public static final int MAX_USER_ATTRIBUTE_SIZE = 255;
    public static final int MAX_LOG_EVENT_ATTRIBUTE_SIZE = 4096; // TODO what if any limit should be imposed on log event attributes?
}
