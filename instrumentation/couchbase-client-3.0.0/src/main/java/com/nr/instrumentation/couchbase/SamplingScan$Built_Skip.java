/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.couchbase;

import com.newrelic.api.agent.weaver.SkipIfPresent;

@SkipIfPresent(originalName = "com.couchbase.client.java.kv.SamplingScan$Built")
public class SamplingScan$Built_Skip {
}
