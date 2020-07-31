/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver.internal;

/**
 * A marker interface we put on new classes in weave packages so that we skip parsing them as they're being loaded.
 * Classes should never directly implement this marker interface.
 */
public interface NewClass {

}
