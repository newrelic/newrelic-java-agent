/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import java.util.Set;

public interface Retransformer {

    void queueRetransform(Set<Class<?>> classesToRetransform);

}
