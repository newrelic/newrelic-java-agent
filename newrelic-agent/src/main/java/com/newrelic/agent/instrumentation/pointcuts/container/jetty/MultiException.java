/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.container.jetty;

import java.util.List;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

/**
 * Jetty uses a MultiException which contains multiple throwables.  The MultiException itself isn't all that meaningful - the
 * good stuff is in its contained throwables.  This interface exposes that list of throwables so we can report stack traces for those.
 */
@InterfaceMixin(originalClassName = "org/eclipse/jetty/util/MultiException")
public interface MultiException {
    List<Throwable> getThrowables();
}
