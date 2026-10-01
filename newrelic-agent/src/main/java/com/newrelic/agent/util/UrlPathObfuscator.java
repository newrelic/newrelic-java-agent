/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.UrlPathObfuscationConfig;
import com.newrelic.agent.service.ServiceFactory;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Class used to obfuscate URL paths based on the supplied UrlPathObfuscationConfig
 * values.
 */
public class UrlPathObfuscator implements AgentConfigListener {
    private static final ConfigHolder DISABLED = new ConfigHolder(null);
    private static volatile UrlPathObfuscator instance = null;
    private volatile ConfigHolder configHolder;

    private UrlPathObfuscator() {
        // This is used in case the class is accessed prior to the config service being spun up
        ConfigHolder holder = DISABLED;
        try {
            ConfigService configService = ServiceFactory.getConfigService();
            holder = new ConfigHolder(configService.getDefaultAgentConfig().getUrlPathObfuscationConfig());
            configService.addIAgentConfigListener(this);
        } catch (Throwable t) {
            Agent.LOG.log(Level.WARNING, "Unable to initialize URL path obfuscation; URL paths will not be " +
                    "obfuscated. Cause: {0}", t.toString());
        }
        configHolder = holder;
    }

    public static UrlPathObfuscator getInstance() {
        if (instance == null) {
            synchronized (UrlPathObfuscator.class) {
                if (instance == null) {
                    instance = new UrlPathObfuscator();
                }
            }
        }

        return instance;
    }

    public String obfuscatePath(String path) {
        if (configHolder.isEnabled && path != null) {
            //TODO
        }

        return path;
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        try {
            configHolder = new ConfigHolder(agentConfig.getUrlPathObfuscationConfig());
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "Unable to apply URL path obfuscation config change; " +
                    "keeping previous settings. Exception: {0}", t.toString());
        }
    }

    private static final class ConfigHolder {
        private final boolean isEnabled;
        private final Pattern regExPattern;
        private final String regExReplacement;

        ConfigHolder(UrlPathObfuscationConfig config) {
            isEnabled = config != null && config.isEnabled();
            regExPattern = config == null ? null : config.getRegExPattern();
            regExReplacement = config == null ? "" : config.getRegExReplacement();
        }
    }
}
