/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

/* (non-javadoc)
 * Note: the "beacon" was a predecessor technology for correlated transaction traces with the browser.
 * Some appearances of the term could be changed to "browser" now.
 */

/**
 * A class that formats the JavaScript header and footer for Real User Monitoring.
 * <p>
 * This class is thread-safe
 */
public class BrowserConfig extends BaseConfig {

    public static final String BROWSER_KEY = "browser_key";
    public static final String BROWSER_LOADER_VERSION = "browser_monitoring.loader_version";
    public static final String JS_AGENT_LOADER = "js_agent_loader";
    public static final String JS_AGENT_FILE = "js_agent_file";
    public static final String BEACON = "beacon";
    public static final String ERROR_BEACON = "error_beacon";
    public static final String APPLICATION_ID = "application_id";

    private static final String HEADER_BEGIN = "\n<script type=\"text/javascript\">";
    private static final String HEADER_END = "</script>";

    private final BrowserFooter footer;
    private final String jsAgentLoader;
    private final String header;

    private BrowserConfig(String appName, Map<String, Object> props) throws Exception {
        super(props);
        // when rum is turned off on the server, none of the required properties come down
        // meaning this will throw an exception
        footer = initBrowserFooter(appName);
        jsAgentLoader = getRequiredProperty(JS_AGENT_LOADER);
        header = HEADER_BEGIN + jsAgentLoader + HEADER_END;
        logVersion(appName);
    }

    private void logVersion(String appName) {
        String version = getProperty(BROWSER_LOADER_VERSION);
        if (version != null) {
            Agent.LOG.log(Level.INFO, MessageFormat.format("Using RUM version {0} for application \"{1}\"", version,
                    appName));
        }
    }

    private BrowserFooter initBrowserFooter(String appName) throws Exception {
        String beacon = getRequiredProperty(BEACON);
        String browserKey = getRequiredProperty(BROWSER_KEY);
        String errorBeacon = getRequiredProperty(ERROR_BEACON);
        String payloadScript = getRequiredProperty(JS_AGENT_FILE);
        String appId = getRequiredProperty(APPLICATION_ID);
        return new BrowserFooter(appName, beacon, browserKey, errorBeacon, payloadScript, appId);
    }

    private String getRequiredProperty(String key) throws Exception {
        Object val = getProperty(key, null);
        if (val == null) {
            String msg = MessageFormat.format("Real User Monitoring value for {0} is missing", key);
            throw new Exception(msg);
        }
        return val.toString();
    }

    public String getBrowserTimingHeader() {
        return header;
    }

    public String getBrowserTimingHeader(String nonce) {
        // nonce should change per request so we cannot pre-build this string
        return "\n<script type=\"text/javascript\" nonce=\""
                + nonce
                + "\">"
                + jsAgentLoader
                + "</script>";
    }

    public String getBrowserTimingFooter(BrowserTransactionState state) {
        return footer.getFooter(state);
    }

    public String getBrowserTimingFooter(BrowserTransactionState state, String nonce) {
        return footer.getFooter(state, nonce);
    }

    public static BrowserConfig createBrowserConfig(String appName, Map<String, Object> settings) throws Exception {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new BrowserConfig(appName, settings);
    }

}
