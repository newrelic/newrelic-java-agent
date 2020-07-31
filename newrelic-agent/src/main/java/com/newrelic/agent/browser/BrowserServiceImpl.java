/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BrowserServiceImpl extends AbstractService implements BrowserService, ConnectionListener {

    private final ConcurrentMap<String, BrowserConfig> browserConfigs = new ConcurrentHashMap<>();
    private volatile BrowserConfig defaultBrowserConfig = null;
    private final String defaultAppName;

    public BrowserServiceImpl() {
        super(BrowserService.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getRPMServiceManager().addConnectionListener(this);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
    }

    @Override
    public BrowserConfig getBrowserConfig(String appName) {
        if (appName == null || appName.equals(defaultAppName)) {
            return defaultBrowserConfig;
        }
        return browserConfigs.get(appName);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void connected(IRPMService rpmService, AgentConfig agentConfig) {
        String appName = rpmService.getApplicationName();
        BrowserConfig browserConfig = BrowserConfigFactory.createBrowserConfig(appName, agentConfig);
        if (appName == null || appName.equals(defaultAppName)) {
            defaultBrowserConfig = browserConfig;
        } else {
            if (browserConfig == null) {
                browserConfigs.remove(appName);
            } else {
                browserConfigs.put(appName, browserConfig);
            }
        }
    }

    @Override
    public void disconnected(IRPMService rpmService) {
        // do nothing
    }

}
