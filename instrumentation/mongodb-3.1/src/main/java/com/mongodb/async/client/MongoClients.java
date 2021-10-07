/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.async.client;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// This instrumentation will cause Segment timeouts and incorrect metrics if it applies to the async driver
@SkipIfPresent
final class MongoClients {}
