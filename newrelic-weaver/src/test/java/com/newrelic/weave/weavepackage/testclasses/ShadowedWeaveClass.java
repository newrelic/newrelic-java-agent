/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.Weave;

/**
 * A class with a {@link Weave} annotation which does not override any options.
 */
@Weave
public class ShadowedWeaveClass {
}
