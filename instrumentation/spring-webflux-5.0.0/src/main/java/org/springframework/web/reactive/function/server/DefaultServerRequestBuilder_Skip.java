/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.server;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * This is here so we don't weave spring webflux 5.1.0 or above
 */
@SkipIfPresent(originalName = "org.springframework.web.reactive.function.server.DefaultServerRequestBuilder")
class DefaultServerRequestBuilder_Skip {
}
