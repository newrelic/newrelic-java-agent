/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.logging.log4j.core.time;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

// This is the same `SkipIfPresent` class in the 2.6 module, which we're now using to gate
// the 2.11 version from applying to anything below 2.11.
@Weave(originalName = "org.apache.logging.log4j.core.time.Instant", type = MatchType.Interface)
public abstract class Instant_Instrumentation {
}