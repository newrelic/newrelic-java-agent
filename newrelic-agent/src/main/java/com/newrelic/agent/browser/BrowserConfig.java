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
import java.util.Locale;
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

    private static final String SCRIPT_BEGIN = "\n<script type=\"text/javascript\"";
    private static final String SCRIPT_END = "</script>";
    private static final String CLOSING_SCRIPT_TAG = "</script";
    static final String JS_AGENT_CONFIG_STR_PREFIX = "window.NREUM||(NREUM={});NREUM.info=";
    private final String jsAgentLoader;
    private final JavaScriptAgentConfig javaScriptAgentConfig;

    private BrowserConfig(String appName, Map<String, Object> props) throws Exception {
        super(props);
        // when rum is turned off on the server, none of the required properties come down
        // meaning this will throw an exception
        javaScriptAgentConfig = initJavaScriptAgentConfig(appName);
        jsAgentLoader = getRequiredProperty(JS_AGENT_LOADER);
        validateJsAgentLoader(appName, jsAgentLoader);
        logVersion(appName);
    }

    public static BrowserConfig createBrowserConfig(String appName, Map<String, Object> settings) throws Exception {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new BrowserConfig(appName, settings);
    }

    public String getBrowserAgentHeaderScript(BrowserTransactionState state) {
        return getBrowserAgentHeaderScript(state, null);
    }

    public String getBrowserAgentHeaderScript(BrowserTransactionState state, String nonce) {
        return SCRIPT_BEGIN + (nonce != null ? " nonce=\"" + escapeAttributeValue(nonce) + "\">" : ">")
                + JS_AGENT_CONFIG_STR_PREFIX
                + javaScriptAgentConfig.getConfigString(state)
                + ";\n"
                + jsAgentLoader
                + SCRIPT_END;
    }

    /**
     * The loader is inserted into the page as the body of a script element without modification. A closing script tag
     * would end that element early and turn the rest of the loader into page markup, so such a loader is rejected.
     */
    private static void validateJsAgentLoader(String appName, String loader) throws Exception {
        if (loader.isEmpty()) {
            throw new Exception(MessageFormat.format("Real User Monitoring value for {0} is empty", JS_AGENT_LOADER));
        }
        if (loader.toLowerCase(Locale.ROOT).contains(CLOSING_SCRIPT_TAG)) {
            String msg = MessageFormat.format("Real User Monitoring value for {0} contains a closing script tag and will not be injected into "
                    + "pages for application \"{1}\". Browser monitoring stays disabled until the agent reconnects with a valid loader.",
                    JS_AGENT_LOADER, appName);
            Agent.LOG.log(Level.WARNING, msg);
            throw new Exception(msg);
        }
    }

    private static String escapeAttributeValue(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private  JavaScriptAgentConfig initJavaScriptAgentConfig(String appName) throws Exception {
        String beacon = getRequiredProperty(BEACON);
        String browserKey = getRequiredProperty(BROWSER_KEY);
        String errorBeacon = getRequiredProperty(ERROR_BEACON);
        String payloadScript = getRequiredProperty(JS_AGENT_FILE);
        String appId = getRequiredProperty(APPLICATION_ID);
        return new JavaScriptAgentConfig(appName, beacon, browserKey, errorBeacon, payloadScript, appId);
    }

    private void logVersion(String appName) {
        String version = getProperty(BROWSER_LOADER_VERSION);
        if (version != null) {
            Agent.LOG.log(Level.INFO, MessageFormat.format("Using RUM version {0} for application \"{1}\"", version,
                    appName));
        }
    }

    private String getRequiredProperty(String key) throws Exception {
        Object val = getProperty(key, null);
        if (val == null) {
            String msg = MessageFormat.format("Real User Monitoring value for {0} is missing", key);
            throw new Exception(msg);
        }
        return val.toString();
    }
}
