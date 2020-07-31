/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package scala.concurrent.impl;

import com.newrelic.api.agent.weaver.SkipIfPresent;

@SkipIfPresent(originalName = "scala.concurrent.impl.Future")
public class Future_Instrumentation {
}
