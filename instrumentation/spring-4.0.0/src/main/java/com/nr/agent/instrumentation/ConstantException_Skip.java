/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * This class is present in Spring 5.0.0 >=
 */
@SkipIfPresent(originalName = "org.springframework.core.Constants$ConstantException")
public class ConstantException_Skip {
}
