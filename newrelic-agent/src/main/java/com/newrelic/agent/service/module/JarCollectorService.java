/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.newrelic.agent.service.Service;

/**
 * Interface for collecting and sending jars to RPM.
 */
public interface JarCollectorService extends Service {
    void harvest(String appName);

    ClassToJarPathSubmitter getClassToJarPathSubmitter();
}
