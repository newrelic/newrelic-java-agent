/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

public enum ApplicationNamePriority {

    NONE, CONTEXT_PATH, CONTEXT_NAME, CONTEXT_PARAM, FILTER_INIT_PARAM, SERVLET_INIT_PARAM, REQUEST_ATTRIBUTE
}
