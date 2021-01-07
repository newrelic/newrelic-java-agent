/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.service.module.JarCollectorService;

public interface JarCollectorConfig {

    /**
     * True if the {@link JarCollectorService} is enabled, else false.
     *
     * @return <code>true</code> if the {@link JarCollectorService} is enabled.
     */
    boolean isEnabled();

    /**
     * True if temp jars should be skipped.
     *
     * @return <code>true</code> if temp jars should be skipped.
     */
    boolean skipTempJars();

    /**
     * The number of class loaders which we should grab jars from.
     *
     * @return The max number of class loaders to look at for jar information.
     */
    int getMaxClassLoaders();

    /**
     * The maximum number of jars to process per second. Must be positive.
     *
     * @return The number of jars to process per second.
     */
    int getJarsPerSecond();

}
