/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.event;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Present in Mongo DB Driver >= 3.1.0.
 */

@SkipIfPresent
public interface CommandListener {
}

