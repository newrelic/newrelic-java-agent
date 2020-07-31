/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.libs.streams;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * This class is here to prevent play-2.4 from applying when play-2.5 is in use
 */
@SkipIfPresent
public class Accumulator {
}
