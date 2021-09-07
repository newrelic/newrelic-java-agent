/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.reactivestreams.client;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// This instrumentation will cause Segment timeouts if it applies to the reactive driver
@SkipIfPresent
final class MongoClients {}
