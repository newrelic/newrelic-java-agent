/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.quartz;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// This class was introduced in Quartz 2.0.
@SkipIfPresent
public class JobDetailImpl_Skip {
}
