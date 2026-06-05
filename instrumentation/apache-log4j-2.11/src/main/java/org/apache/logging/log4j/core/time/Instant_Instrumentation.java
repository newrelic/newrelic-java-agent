/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.logging.log4j.core.time;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

// Instant was introduced in log4j 2.11.0. The weaver will fail to resolve this class on older
// versions, preventing the 2.11 module from applying to log4j 2.6-2.10.
@Weave(originalName = "org.apache.logging.log4j.core.time.Instant", type = MatchType.Interface)
public abstract class Instant_Instrumentation {
}