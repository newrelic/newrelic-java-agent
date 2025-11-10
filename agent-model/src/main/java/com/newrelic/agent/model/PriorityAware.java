/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

/**
 * Simple interface to grab a priority (float) value from an object.
 */
public interface PriorityAware {

    float getPriority();

}
