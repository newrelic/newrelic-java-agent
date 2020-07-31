/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Set;

public interface BrowserMonitoringConfig {

    /**
     * If the auto_instrument property is <code>true</code>, Real User Monitoring instrumentation is added
     * automatically.
     *
     * @return <code>true</code> if auto instrumentation is enabled
     */
    boolean isAutoInstrumentEnabled();

    /**
     * A set of pages which should be disabled for auto rum.
     *
     * @return Pages which should be disabled for auto rum.
     */
    Set<String> getDisabledAutoPages();

    /**
     * The type of loader to send. Valid values are 'rum' (small loader supporting only RUM functionality), 'full'
     * (larger loader containing JS Errors code and to-be-added features), and 'none' (don't send any JS).
     *
     * @return This should be rum, full, or none.
     */
    String getLoaderType();

    /**
     * We plan to use this setting to control whether non-minified JS should be sent for debugging purposes.
     *
     * @return True if debug should be turned off, else false.
     */
    boolean isDebug();

    /**
     * True forces the use of HTTPS instrumentation on HTTP pages.
     *
     * @return True or False.
     */
    boolean isSslForHttp();

    /**
     * True if this field sslForHttp is explicitly set.
     *
     * @return True if this field sslForHttp is explicitly set, else false.
     */
    boolean isSslForHttpSet();

    /**
     * False means return a RUM footer only once per transaction. Subsequent requests return an empty string. True means
     * return a RUM footer every time.
     *
     * The last RUM footer in the HTML response is the one used by the browser agent.
     */
    boolean isAllowMultipleFooters();

}
