/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.weaver.Weave;

/**
 * This class is present in Spring 4.0.0 >= and this is here to make the verifier happy
 */
@Weave(originalName = "org.springframework.web.bind.annotation.RestController")
public class RestController {
}
