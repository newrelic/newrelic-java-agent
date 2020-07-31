/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = "com.newrelic.agent.instrumentation.Test4")
public interface InterfaceMixinMissingMethodTest4 {

    void methodDoesNotExist();
}
