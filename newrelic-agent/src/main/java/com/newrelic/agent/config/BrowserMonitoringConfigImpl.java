/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BrowserMonitoringConfigImpl extends BaseConfig implements BrowserMonitoringConfig {

    public static final String AUTO_INSTRUMENT = "auto_instrument";
    public static final String DISABLE_AUTO_PAGES = "disabled_auto_pages";
    public static final String SSL_FOR_HTTP = "ssl_for_http";
    public static final String LOADER_TYPE = "loader";
    public static final String DEBUG = "debug";
    public static final String ALLOW_MULTIPLE_FOOTERS = "allow_multiple_footers";

    public static final boolean DEFAULT_AUTO_INSTRUMENT = true;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.browser_monitoring.";
    public static final String DEFAULT_LOADER_TYPE = "rum";
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_SSL_FOR_HTTP = true;
    public static final boolean DEFAULT_ALLOW_MULTIPLE_FOOTERS = false;

    private final boolean auto_instrument;
    private final Set<String> disabledAutoPages;
    private final String loaderType;
    private final boolean debug;
    private final boolean sslForHttp;
    private final boolean isSslForHttpSet;
    private final boolean multipleFooters;

    private BrowserMonitoringConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        auto_instrument = getProperty(AUTO_INSTRUMENT, DEFAULT_AUTO_INSTRUMENT);
        disabledAutoPages = Collections.unmodifiableSet(new HashSet<>(getUniqueStrings(DISABLE_AUTO_PAGES)));
        loaderType = getProperty(LOADER_TYPE, DEFAULT_LOADER_TYPE);
        debug = getProperty(DEBUG, DEFAULT_DEBUG);
        Boolean sslForHttpTmp = getProperty(SSL_FOR_HTTP);
        isSslForHttpSet = !(sslForHttpTmp == null);
        sslForHttp = isSslForHttpSet ? sslForHttpTmp : DEFAULT_SSL_FOR_HTTP;
        multipleFooters = getProperty(ALLOW_MULTIPLE_FOOTERS, DEFAULT_ALLOW_MULTIPLE_FOOTERS);
    }

    @Override
    public boolean isAutoInstrumentEnabled() {
        return auto_instrument;
    }

    static BrowserMonitoringConfig createBrowserMonitoringConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new BrowserMonitoringConfigImpl(settings);
    }

    @Override
    public Set<String> getDisabledAutoPages() {
        return disabledAutoPages;
    }

    @Override
    public String getLoaderType() {
        return loaderType;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isSslForHttp() {
        return sslForHttp;
    }

    @Override
    public boolean isSslForHttpSet() {
        return isSslForHttpSet;
    }

    @Override
    public boolean isAllowMultipleFooters() {
        return multipleFooters;
    }

}
