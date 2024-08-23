/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */


package slick.util;

import com.newrelic.api.agent.weaver.SkipIfPresent;

@SkipIfPresent(originalName = "slick.util.AsyncExecutor$DefaultAsyncExecutor")
public class SkipDefaultAsyncExecutor {
}
