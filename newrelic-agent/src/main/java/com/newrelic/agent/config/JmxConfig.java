/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collection;

public interface JmxConfig {

    /**
     * True is the jmx service is enabled, else false.
     *
     * @return <code>true</code> if the jmx service is enabled.
     */
    boolean isEnabled();

    /**
     * These strings should match the prefix found in the Jmx Framework. Any matching strings should be disabled.
     *
     * @return Strings which match Jmx Framework prefixes that should be disabled.
     */
    Collection<String> getDisabledJmxFrameworks();

    /**
     * Returns true if the LinkingMetadataMBean should be registered in the platform
     * MBean server.
     */
    boolean registerLinkingMetadataMBean();

    boolean enableIteratedObjectNameKeys();
}
