/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Play v1 instrumentation is implemented using its own set of pointcuts that don't work well with our async APIs. This
 * class is present in Play v1 but not v2, and will cause this module NOT to load if the customer is using Play v1.
 */
@SkipIfPresent
public class CorePlugin {
}
