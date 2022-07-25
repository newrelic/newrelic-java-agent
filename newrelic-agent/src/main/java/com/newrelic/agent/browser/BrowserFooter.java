/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Obfuscator;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BrowserFooter {
    // location of the beacon (hostname[:port]) (always present)
    private static final String BEACON_KEY = "beacon";
    // JS error beacon location (always present)
    private static final String ERROR_BEACON_KEY = "errorBeacon";
    // license key (always present)
    private static final String LICENSE_KEY = "licenseKey";
    // id(s) of the application (always present)
    private static final String APPLICATION_ID_KEY = "applicationID";
    // name of the transaction (always present)
    private static final String TRANSACTION_NAME_KEY = "transactionName";
    // Queue time in milliseconds (always present)
    private static final String QUEUE_TIME_KEY = "queueTime";
    // Application time in milliseconds (always present)
    private static final String APP_TIME_KEY = "applicationTime";
    // when there is an agentToken and applicationTime is above the transaction trace threshold (optional)
    private static final String TRAN_TRACE_GUID_KEY = "ttGuid";
    // when it is present in an "NRAGENT" cookie (and has valid format) - optional
    private static final String AGENT_TOKEN_KEY = "agentToken";
    // user and agent parameters (user, account, product, etc)
    private static final String ATTS_KEY = "atts";
    // True or false to force the use or non-use of HTTPS instrumentation on HTTP pages (only when explicitly set)
    private static final String SSL_FOR_HTTP_KEY = "sslForHttp";
    // the JS agent payload script (always present)
    private static final String AGENT_PAYLOAD_SCRIPT_KEY = "agent";

    // Used by functional tests
    static final String FOOTER_JS_START = "window.NREUM||(NREUM={});NREUM.info=";
    public static final String FOOTER_START_SCRIPT = "\n<script type=\"text/javascript\">" + FOOTER_JS_START;
    public static final String FOOTER_END = "</script>";

    private final String beacon;
    private final String browserKey;
    private final String errorBeacon;
    private final String payloadScript;
    private final String appId;
    private final Boolean isSslForHttp;

    public BrowserFooter(String appName, String pBeacon, String pBrowserKey, String pErrorBeacon, String pPayloadScript,
            String pAppId) {
        beacon = pBeacon;
        browserKey = pBrowserKey;
        errorBeacon = pErrorBeacon;
        payloadScript = pPayloadScript;
        appId = pAppId;
        BrowserMonitoringConfig config = ServiceFactory.getConfigService().getAgentConfig(appName).getBrowserMonitoringConfig();
        if (config.isSslForHttpSet()) {
            isSslForHttp = config.isSslForHttp();
        } else {
            isSslForHttp = null;
        }
    }

    public String getFooter(BrowserTransactionState state) {
        String jsonString = jsonToString(createMapWithData(state));
        if (jsonString != null) {
            return FOOTER_START_SCRIPT + jsonString + FOOTER_END;
        } else {
            return "";
        }
    }

    String getFooter(BrowserTransactionState state, String nonce) {
        String jsonString = jsonToString(createMapWithData(state));
        if (jsonString != null) {
            return "\n<script type=\"text/javascript\" nonce=\""
                    + nonce
                    + "\">"
                    + FOOTER_JS_START
                    + jsonString
                    + FOOTER_END;
        } else {
            return "";
        }
    }

    private String jsonToString(Map<String, ?> map) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Writer out = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            JSONObject.writeJSONString(map, out);
            out.flush();
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Agent.LOG.log(Level.INFO, "An error occurred when creating the rum footer. Issue:" + e.getMessage());
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.log(Level.FINEST, "Exception when creating rum footer. ", e);
            }
            return null;
        }
    }

    private Map<String, Object> createMapWithData(BrowserTransactionState state) {
        Map<String, Object> output = new HashMap<>();
        // these come from the collector
        output.put(BEACON_KEY, beacon);
        output.put(ERROR_BEACON_KEY, errorBeacon);
        output.put(LICENSE_KEY, browserKey);
        output.put(APPLICATION_ID_KEY, appId);
        output.put(AGENT_PAYLOAD_SCRIPT_KEY, payloadScript);
        // these come directly form the state and should never be null
        output.put(QUEUE_TIME_KEY, state.getExternalTimeInMilliseconds());
        output.put(APP_TIME_KEY, state.getDurationInMilliseconds());
        output.put(TRANSACTION_NAME_KEY, obfuscate(state.getTransactionName()));
        // these should not be sent if null
        addToMapIfNotNullOrEmpty(output, SSL_FOR_HTTP_KEY, isSslForHttp);
        // attributes have to be filtered through the service
        addToMapIfNotNullAndObfuscate(output, ATTS_KEY, getAttributes(state));
        return output;
    }

    // protected for testing
    protected static Map<String, Object> getAttributes(BrowserTransactionState state) {
        Map<String, Object> atts;
        if (ServiceFactory.getAttributesService().isAttributesEnabledForBrowser(state.getAppName())) {
            Map<String, ?> userAtts = ServiceFactory.getAttributesService().filterBrowserAttributes(
                    state.getAppName(), state.getUserAttributes());
            Map<String, ?> agentAtts = ServiceFactory.getAttributesService().filterBrowserAttributes(
                    state.getAppName(), state.getAgentAttributes());
            atts = new HashMap<>(3);
            // user attributes should have already been filtered for high security - this is just extra protection
            // high security is per an account - meaning it can not be different for various application names within a
            // JVM - so we can just check the default agent config
            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity() && !userAtts.isEmpty()) {
                atts.put("u", userAtts);
            }
            if (!agentAtts.isEmpty()) {
                atts.put("a", agentAtts);
            }
        } else {
            atts = Collections.emptyMap();
        }
        return atts;
    }

    private void addToMapIfNotNullOrEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    private void addToMapIfNotNullOrEmpty(Map<String, Object> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void addToMapIfNotNullAndObfuscate(Map<String, Object> map, String key, Map<String, ?> value) {
        if (value != null && !value.isEmpty()) {
            String output = jsonToString(value);
            if (output != null && !output.isEmpty()) {
                map.put(key, obfuscate(output));
            }
        }
    }

    private String obfuscate(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }
        String licenseKey = ServiceFactory.getConfigService().getDefaultAgentConfig().getLicenseKey();
        if (licenseKey == null) {
            throw new NullPointerException("License Key was null. It must be set before obfuscating.");
        }

        return Obfuscator.obfuscateNameUsingKey(name, licenseKey.substring(0, 13));
    }

}
