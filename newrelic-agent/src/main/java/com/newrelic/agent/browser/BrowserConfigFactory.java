/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.BaseConfig;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

class BrowserConfigFactory {

    static BrowserConfig createBrowserConfig(String appName, AgentConfig agentConfig) {
        try {
            BrowserConfig browserConfig = createTheBrowserConfig(appName, ((BaseConfig) agentConfig).getProperties());
            boolean autoInstrumentEnabled = agentConfig.getBrowserMonitoringConfig().isAutoInstrumentEnabled();
            String msg = MessageFormat.format("Real user monitoring is enabled for application {0}. Auto instrumentation is {1}.", appName,
                    autoInstrumentEnabled ? "enabled" : "disabled");
            Agent.LOG.info(msg);
            return browserConfig;
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to configure application \"{0}\" for Real User Monitoring: {1}", appName, e);
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else {
                Agent.LOG.finer(msg);
            }
            Agent.LOG.info(MessageFormat.format("Real user monitoring is not enabled for application \"{0}\"", appName));
            return null;
        }
    }

    private static BrowserConfig createTheBrowserConfig(String appName, Map<String, Object> serverData) throws Exception {
        Map<String, Object> settings = new HashMap<>();
        mergeBrowserSettings(settings, serverData);
        Map<String, Object> agentData = AgentConfigFactory.getAgentData(serverData);
        // server-side configuration may override browser settings
        mergeBrowserSettings(settings, agentData);
        return BrowserConfig.createBrowserConfig(appName, settings);
    }

    private static void mergeBrowserSettings(Map<String, Object> settings, Map<String, Object> data) {
        if (data == null) {
            return;
        }
        mergeSetting(BrowserConfig.BROWSER_KEY, settings, data);
        mergeSetting(BrowserConfig.BROWSER_LOADER_VERSION, settings, data);
        mergeSetting(BrowserConfig.JS_AGENT_LOADER, settings, data);
        mergeSetting(BrowserConfig.JS_AGENT_FILE, settings, data);
        mergeSetting(BrowserConfig.BEACON, settings, data);
        mergeSetting(BrowserConfig.ERROR_BEACON, settings, data);
        mergeSetting(BrowserConfig.APPLICATION_ID, settings, data);
    }

    private static void mergeSetting(String currentSetting, Map<String, Object> settings, Map<String, Object> data) {
        Object val = data.get(currentSetting);
        if (val != null) {
            settings.put(currentSetting, val);
        }
    }

}
