/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.concurrent.atomic.AtomicBoolean;

public class DebugFlag {
    public static final AtomicBoolean tokenEnabled = new AtomicBoolean();
}
