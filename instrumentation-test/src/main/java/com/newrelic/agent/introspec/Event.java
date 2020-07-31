/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.util.Map;

/**
 * The Java agent currently collects three types of Event data: Transaction, Error, and Custom. All of these event
 * types will be represented by this event interface.
 */
public interface Event {

    String getType();

    Map<String, Object> getAttributes();
}