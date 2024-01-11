/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
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

public class JavaScriptAgentConfig {
    private static final String BEACON_KEY = "beacon";
    private static final String ERROR_BEACON_KEY = "errorBeacon";
    private static final String LICENSE_KEY = "licenseKey";
    private static final String APPLICATION_ID_KEY = "applicationID";
    private static final String TRANSACTION_NAME_KEY = "transactionName";
    private static final String QUEUE_TIME_KEY = "queueTime";
    private static final String APP_TIME_KEY = "applicationTime";
    private static final String ATTS_KEY = "atts";
    private static final String SSL_FOR_HTTP_KEY = "sslForHttp";
    private static final String AGENT_PAYLOAD_SCRIPT_KEY = "agent";

    private final String beacon;
    private final String browserKey;
    private final String errorBeacon;
    private final String payloadScript;
    private final String appId;
    private final Boolean isSslForHttp;

    public JavaScriptAgentConfig(String appName, String beacon, String browserKey, String errorBeacon, String payloadScript,
            String appId) {
        this.beacon = beacon;
        this.browserKey = browserKey;
        this.errorBeacon = errorBeacon;
        this.payloadScript = payloadScript;
        this.appId = appId;
        BrowserMonitoringConfig config = ServiceFactory.getConfigService().getAgentConfig(appName).getBrowserMonitoringConfig();
        isSslForHttp = config.isSslForHttpSet() ? config.isSslForHttp() : null;
    }

    public String getConfigString(BrowserTransactionState state) {
        return mapToJsonString(createJavaScriptAgentConfigMap(state));
    }

    private String mapToJsonString(Map<String, ?> map) {
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

    private Map<String, Object> createJavaScriptAgentConfigMap(BrowserTransactionState state) {
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

    private void addToMapIfNotNullOrEmpty(Map<String, Object> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void addToMapIfNotNullAndObfuscate(Map<String, Object> map, String key, Map<String, ?> value) {
        if (value != null && !value.isEmpty()) {
            String output = mapToJsonString(value);
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
