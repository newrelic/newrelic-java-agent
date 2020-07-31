/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

/**
 * Simple interface to grab a priority (float) value from an object and to determine if this app was the "decider".
 */
public interface PriorityAware {

    boolean decider();

    float getPriority();

}
