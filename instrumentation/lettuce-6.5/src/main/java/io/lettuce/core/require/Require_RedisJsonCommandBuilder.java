/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.lettuce.core.require;

import com.newrelic.api.agent.weaver.Weave;

/***
 * This is a convenience version-matching-control class.
 * We do not want this instrumentation module to apply to lettuce versions < 6.5.
 * RedisJSONCommandBuilder was introduced in lettuce 6.5 and will force earlier versions not to match.
 *
 * This weave class provides no instrumentation and can be safely removed if another matching control mechanism is identified.
 */

@Weave(originalName = "io.lettuce.core.RedisJsonCommandBuilder")
class Require_RedisJsonCommandBuilder {
    //Here for matching purposes only.
}
