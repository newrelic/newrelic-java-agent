/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.weaver.SkipIfPresent;
import com.newrelic.api.agent.weaver.Weave;

// this exists in Spring 4.3 and on but not before
@Weave(originalName = "org.springframework.web.bind.annotation.GetMapping")
public class GetMapping_AntiSkip {
}
