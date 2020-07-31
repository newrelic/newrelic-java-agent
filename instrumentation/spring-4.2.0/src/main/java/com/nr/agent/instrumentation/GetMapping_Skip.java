/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// this exists in Spring 5
@SkipIfPresent(originalName = "org.springframework.web.bind.annotation.GetMapping")
public class GetMapping_Skip {
}
